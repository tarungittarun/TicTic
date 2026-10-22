package app.space.feature.messages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class MessagesRulesTest {

    private static MessagesRules.Message m(String id, String address, long when, boolean incoming, boolean read) {
        return new MessagesRules.Message(id, address, "body " + id, when, incoming, read);
    }

    @Test public void formattingDifferencesAndACountryCodeFoldIntoOneThread() {
        // The same person arrives in three shapes from three senders. Without folding them
        // one conversation renders as three, which is the most visible failure an SMS app
        // can have.
        String a = MessagesRules.normalise("+91 98765 43210");
        String b = MessagesRules.normalise("098765 43210");
        String c = MessagesRules.normalise("9876543210");
        assertEquals(a, c);
        assertEquals(b, c);
        assertEquals("9876543210", c);
    }

    @Test public void distinctNumbersStayDistinct() {
        assertFalse(MessagesRules.normalise("9876543210").equals(MessagesRules.normalise("9876543211")));
        // A nine-digit tail would merge these two: both end in 876543210. Two people would
        // then share one thread and each would read the other's messages.
        assertFalse(MessagesRules.normalise("9876543210").equals(MessagesRules.normalise("8876543210")));
    }

    @Test public void alphanumericSenderIdsStayDistinctFromEachOther() {
        // India's DLT headers carry no digits. Reducing them to digits yields "", which would
        // fold every service sender into a single "Unknown sender" thread.
        assertEquals("VKTICTIC", MessagesRules.normalise("VK-TICTIC"));
        assertEquals("VKTICTIC", MessagesRules.normalise("vk-tictic"));
        assertFalse(MessagesRules.normalise("VK-TICTIC").equals(MessagesRules.normalise("AX-PROMO")));
        assertFalse(MessagesRules.normalise("VK-TICTIC").isEmpty());
    }

    @Test public void anAddresslessSenderGroupsUnderOneUnknownThread() {
        // Carrier and service short codes often arrive with no address. Each becoming its own
        // conversation would bury real threads.
        assertEquals("", MessagesRules.normalise(null));
        assertEquals("", MessagesRules.normalise(""));
        assertEquals("Unknown sender", MessagesRules.threadLabel(""));
        assertEquals("Unknown sender", MessagesRules.threadLabel(null));
        assertEquals("+91 98765 43210", MessagesRules.threadLabel(" +91 98765 43210 "));
    }

    @Test public void shortCodesAndSenderHeadersAreRecognised() {
        assertTrue(MessagesRules.isShortCode("VK-TICTIC"));
        assertTrue(MessagesRules.isShortCode("56161"));
        assertTrue(MessagesRules.isShortCode(" 56161 "));
        assertFalse(MessagesRules.isShortCode("+919876543210"));
        assertFalse(MessagesRules.isShortCode(""));
        assertFalse(MessagesRules.isShortCode(null));
    }

    @Test public void threadsAreNewestFirstAndReadOldestFirst() {
        List<MessagesRules.Thread> threads = MessagesRules.threads(Arrays.asList(
                m("1", "111", 100, true, true),
                m("2", "222", 900, true, false),
                m("3", "111", 500, false, true)));
        assertEquals(2, threads.size());
        assertEquals("222", threads.get(0).address);
        assertEquals("111", threads.get(1).address);
        // Within a conversation the order is chronological, which is how it reads.
        List<MessagesRules.Message> first = threads.get(1).messages;
        assertEquals(2, first.size());
        assertEquals("1", first.get(0).id);
        assertEquals("3", first.get(1).id);
    }

    @Test public void unreadCountsOnlyIncomingUnreadMessages() {
        List<MessagesRules.Thread> threads = MessagesRules.threads(Arrays.asList(
                m("1", "111", 100, true, false),
                m("2", "111", 200, true, false),
                m("3", "111", 300, false, false)));
        assertEquals(2, threads.get(0).unread);
        assertEquals(2, MessagesRules.totalUnread(Arrays.asList(
                m("1", "111", 100, true, false), m("2", "222", 200, true, false),
                m("3", "333", 300, false, false))));
    }

    @Test public void previewCollapsesWhitespaceAndTruncates() {
        assertEquals("a b c", MessagesRules.singleLine("  a\n\n  b   c  "));
        assertEquals("", MessagesRules.singleLine(null));
        StringBuilder long_ = new StringBuilder();
        for (int i = 0; i < 200; i++) long_.append('x');
        String out = MessagesRules.singleLine(long_.toString());
        assertEquals(80, out.length());
        assertTrue(out.endsWith("\u2026"));
    }

    @Test public void anEmptyBodyIsRefusedBecauseItStillCostsASend() {
        assertFalse(MessagesRules.isSendable(""));
        assertFalse(MessagesRules.isSendable("   "));
        assertFalse(MessagesRules.isSendable(null));
        assertTrue(MessagesRules.isSendable("hi"));
        StringBuilder huge = new StringBuilder();
        for (int i = 0; i < 1200; i++) huge.append('x');
        assertFalse(MessagesRules.isSendable(huge.toString()));
    }

    @Test public void segmentCountMatchesWhatTheCarrierCharges() {
        assertEquals(0, MessagesRules.segmentCount(""));
        assertEquals(1, MessagesRules.segmentCount(repeat("a", 160)));
        assertEquals(2, MessagesRules.segmentCount(repeat("a", 161)));
        // Any non-GSM character switches the message to UCS-2, which halves the segment
        // size. Showing 160 here would understate the charge for an emoji message.
        assertEquals(1, MessagesRules.segmentCount(repeat("\u0915", 70)));
        assertEquals(2, MessagesRules.segmentCount(repeat("\u0915", 71)));
        assertFalse(MessagesRules.isSevenBit("hi \u0915"));
        assertTrue(MessagesRules.isSevenBit("hi there"));
    }

    @Test public void searchCoversBodyAndAddressAndBlankMatchesEverything() {
        List<MessagesRules.Message> all = Arrays.asList(
                new MessagesRules.Message("1", "111", "rent due", 1, true, true),
                new MessagesRules.Message("2", "222", "lunch?", 2, true, true));
        assertEquals(1, MessagesRules.filter(all, "RENT").size());
        assertEquals(1, MessagesRules.filter(all, "222").size());
        assertEquals(2, MessagesRules.filter(all, "").size());
        assertEquals(2, MessagesRules.filter(all, null).size());
    }

    @Test public void withReadPreservesIdentity() {
        MessagesRules.Message original = m("1", "111", 100, true, false);
        MessagesRules.Message read = original.withRead(true);
        assertEquals("1", read.id);
        assertEquals(100, read.timestamp);
        assertTrue(read.read);
        assertFalse(original.read);
    }

    private static String repeat(String s, int times) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < times; i++) out.append(s);
        return out.toString();
    }
}
