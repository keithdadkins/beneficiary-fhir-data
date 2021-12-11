package gov.cms.bfd.pipeline.rda.grpc.sink.concurrent;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SequenceNumberTrackerTest {
  @Test
  public void defaultValueReturnedWhenEmpty() {
    final var tracker = new SequenceNumberTracker(100);
    assertEquals(100, tracker.getHighestWrittenSequenceNumber());
  }

  @Test
  public void maxValueReturnedOnceEmpty() {
    final var tracker = new SequenceNumberTracker(100);
    tracker.addActiveSequenceNumber(101);
    tracker.addActiveSequenceNumber(102);
    tracker.removeWrittenSequenceNumber(102);
    tracker.removeWrittenSequenceNumber(101);
    assertEquals(102, tracker.getHighestWrittenSequenceNumber());
  }

  @Test
  public void tracksRemovedNumbersProperly() {
    final var tracker = new SequenceNumberTracker(100);
    tracker.addActiveSequenceNumber(101);
    assertEquals(100, tracker.getHighestWrittenSequenceNumber());

    tracker.addActiveSequenceNumber(102);
    assertEquals(100, tracker.getHighestWrittenSequenceNumber());

    tracker.addActiveSequenceNumber(103);
    assertEquals(100, tracker.getHighestWrittenSequenceNumber());

    tracker.removeWrittenSequenceNumber(102);
    assertEquals(100, tracker.getHighestWrittenSequenceNumber());

    tracker.removeWrittenSequenceNumber(101);
    assertEquals(102, tracker.getHighestWrittenSequenceNumber());

    tracker.addActiveSequenceNumber(104);
    assertEquals(102, tracker.getHighestWrittenSequenceNumber());

    tracker.removeWrittenSequenceNumber(103);
    assertEquals(103, tracker.getHighestWrittenSequenceNumber());

    tracker.removeWrittenSequenceNumber(104);
    assertEquals(104, tracker.getHighestWrittenSequenceNumber());
  }
}
