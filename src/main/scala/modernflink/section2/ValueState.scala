package modernflink.section2

import modernflink.model.{Deposit, DepositEventGenerator}
import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.util.Collector
import org.apache.flinkx.api.{DataStream, StreamExecutionEnvironment}
import org.apache.flinkx.api.function.StatefulFunction
import org.apache.flinkx.api.serializers.*

import java.time.{Duration, Instant}

class CountDeposit() extends KeyedProcessFunction[String, Deposit, String]:
  var depositStateCounter: ValueState[Int] = _

  override def open(parameters: Configuration): Unit =
  // initialize state
  depositStateCounter = getRuntimeContext.getState(new ValueStateDescriptor[Int]("count state", classOf[Int]))

  override def processElement(value: Deposit, ctx: KeyedProcessFunction[String, Deposit, String]#Context, out: Collector[String]): Unit =
    if depositStateCounter.value() == null.asInstanceOf[Int] then
      depositStateCounter.update(1) // default value
    // get current state
    val depositCountByCurrency = depositStateCounter.value()
    // update current state
    depositStateCounter.update(depositCountByCurrency + 1)
    out.collect(s"Total count of deposits in ${value.currency}: $depositCountByCurrency")

@main def valueStateDemo(): Unit =

  val env = StreamExecutionEnvironment.getExecutionEnvironment

  val depositData = env
    .addSource(
      new DepositEventGenerator(
        sleepSeconds = 1, startTime = Instant.parse("2023-08-13T00:00:00.00Z")
      )
    )

  val countDepositStream = depositData
    .keyBy(_.currency)
//    .process(CountDeposit())

    .mapWithState[String, Int]:
      (deposit, state) =>
        (
          s"Total count of deposits in ${deposit.currency}: ${state.getOrElse(1)}", //output
            state.orElse(Some(1)) // Set initial default
              .map(_ + 1) // Update state
        )

  countDepositStream.print()
  env.execute()