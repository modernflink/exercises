package modernflink.section2

import modernflink.model.{SubscriptionEventsGenerator, SubscriptionEvent}
import org.apache.flink.api.common.eventtime.{SerializableTimestampAssigner, WatermarkStrategy}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.windowing.assigners.{TumblingEventTimeWindows, TumblingProcessingTimeWindows}
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.util.Collector
import org.apache.flinkx.api.serializers.*
import org.apache.flinkx.api.StreamExecutionEnvironment
import java.time.{Duration, Instant}
import modernflink.model.instantTypeInfo

val env = StreamExecutionEnvironment.getExecutionEnvironment

val subscriptionEvent = env.addSource(
  new SubscriptionEventsGenerator(
    sleepSeconds = 1, startTime = Instant.parse("2023-08-13T00:00:00.00Z")
  )
)

// Processing Time
def processingTimeDemo(): Unit =
  val eventStreamOne = subscriptionEvent
    .keyBy(_.userId)
    .window(TumblingProcessingTimeWindows.of(Time.seconds(5)))
    .apply(
      (
        userId,
        timeWindow,
        events,
        collector: Collector[(String, String)]
      ) => {
        collector.collect(
          userId,
          events
            .map(e =>
              s"Processing Time Window ${timeWindow.getStart} - ${timeWindow.getEnd}: ${e.getClass.getSimpleName} at ${e.time}"
            )
            .mkString
        )
      }
    )

  eventStreamOne.print()
  env.execute()

// Event Time
def eventTimeDemo(): Unit =
  val withWatermarks = subscriptionEvent
    .assignTimestampsAndWatermarks(
      WatermarkStrategy
        .forBoundedOutOfOrderness(Duration.ofSeconds(10))
        .withTimestampAssigner(
          new SerializableTimestampAssigner[SubscriptionEvent]:
              override def extractTimestamp(element: SubscriptionEvent, recordTimestamp: Long): Long = element.time.toEpochMilli
            )
    )

  val eventStreamTwo = withWatermarks
    .keyBy(_.userId)
    .window(TumblingEventTimeWindows.of(Time.seconds(5)))
    .apply(
      (
        userId,
        timeWindow,
        events,
        collector: Collector[(String, String)]
      ) => {
        collector.collect(
          userId,
          events
            .map(e =>
              s"Event Time Window ${timeWindow.getStart} - ${timeWindow.getEnd}: ${e.getClass.getSimpleName} at ${e.time}"
            )
            .mkString
        )
      }
    )

  eventStreamTwo.print()
  env.execute()

@main def processingTimeEventTime() =
//  processingTimeDemo()
  eventTimeDemo()


