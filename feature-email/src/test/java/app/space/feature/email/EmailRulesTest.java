package app.space.feature.email;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EmailRulesTest {

    @Test public void aCommaInsideADisplayNameDoesNotSplitTheRecipient() {
        // A naive split on commas breaks "Doe, Jane <jane@example.com>" into two invalid
        // halves, and the send then fails with a message that does not explain why.
        List<String> out = EmailRules.splitRecipients("\"Doe, Jane\" <jane@example.com>, other@example.com");
        assertEquals(2, out.size());
        assertEquals("jane@example.com", EmailRules.addressOf(out.get(0)));
        assertEquals("other@example.com", EmailRules.addressOf(out.get(1)));
    }

    @Test public void angleBracketsProtectCommasToo() {
        List<String> out = EmailRules.splitRecipients("Jane <jane@example.com>, Bob <bob@example.com>");
        assertEquals(2, out.size());
        assertEquals("Jane", EmailRules.nameOf(out.get(0)));
        assertEquals("Bob", EmailRules.nameOf(out.get(1)));
    }

    @Test public void bareAddressesAndEmptyFieldsAreHandled() {
        assertEquals(Collections.singletonList("a@b.com"), EmailRules.splitRecipients(" a@b.com "));
        assertTrue(EmailRules.splitRecipients("").isEmpty());
        assertTrue(EmailRules.splitRecipients(null).isEmpty());
        assertEquals(1, EmailRules.splitRecipients("a@b.com,,,").size());
    }

    @Test public void nameAndAddressExtractionFallsBackRatherThanGoingBlank() {
        assertEquals("jane@example.com", EmailRules.addressOf("Jane <jane@example.com>"));
        assertEquals("jane@example.com", EmailRules.addressOf("jane@example.com"));
        assertEquals("Jane", EmailRules.nameOf("Jane <jane@example.com>"));
        // No display name means the address is shown, so a row is never blank.
        assertEquals("jane@example.com", EmailRules.nameOf("<jane@example.com>"));
        assertEquals("jane@example.com", EmailRules.nameOf("jane@example.com"));
        assertEquals("", EmailRules.addressOf(null));
    }

    @Test public void validationRejectsTheShapesThatMakeASendFailSilently() {
        assertTrue(EmailRules.isValidAddress("jane@example.com"));
        assertTrue(EmailRules.isValidAddress("jane+tag@example.co.uk"));
        assertTrue(EmailRules.isValidAddress("Jane <jane@example.com>"));
        assertFalse(EmailRules.isValidAddress("jane"));
        assertFalse(EmailRules.isValidAddress("@example.com"));
        assertFalse(EmailRules.isValidAddress("jane@"));
        assertFalse(EmailRules.isValidAddress("jane@example"));
        assertFalse(EmailRules.isValidAddress("ja ne@example.com"));
        assertFalse(EmailRules.isValidAddress("jane@@example.com"));
        assertFalse(EmailRules.isValidAddress("jane@exa..mple.com"));
        assertFalse(EmailRules.isValidAddress(""));
        assertFalse(EmailRules.isValidAddress(null));
    }

    @Test public void theFirstInvalidRecipientIsReportedSoTheUserKnowsWhichOne() {
        assertNull(EmailRules.firstInvalid(Arrays.asList("a@b.com", "c@d.com")));
        assertEquals("nope", EmailRules.firstInvalid(Arrays.asList("a@b.com", "nope", "c@d.com")));
        assertNull(EmailRules.firstInvalid(null));
    }

    @Test public void repliesThreadByStrippingAccumulatedPrefixes() {
        // Re: Fwd: Re: accumulates over a long thread and clients differ in case, so the
        // stripping must repeat and be case-insensitive or the reply starts a new thread.
        assertEquals("lunch", EmailRules.threadKey("Re: Lunch"));
        assertEquals("lunch", EmailRules.threadKey("Fwd: lunch"));
        assertEquals("lunch", EmailRules.threadKey("re: FW: Re: Lunch"));
        assertEquals("", EmailRules.threadKey(null));
    }

    @Test public void replySubjectAddsReOnlyWhenItIsMissing() {
        assertEquals("Re: Lunch", EmailRules.replySubject("Lunch"));
        assertEquals("Re: Lunch", EmailRules.replySubject("Re: Lunch"));
        assertEquals("re: lunch", EmailRules.replySubject("re: lunch"));
        assertEquals("Re: ", EmailRules.replySubject(""));
    }

    @Test public void conversationsGroupByThreadAndSortNewestFirst() {
        List<EmailRules.Conversation> out = EmailRules.conversations(Arrays.asList(
                mail("1", "Lunch", 100), mail("2", "Re: Lunch", 500), mail("3", "Invoice", 300)));
        assertEquals(2, out.size());
        assertEquals("lunch", out.get(0).key);
        assertEquals(2, out.get(0).size());
        assertEquals("invoice", out.get(1).key);
        // The conversation's own messages are newest first too.
        assertEquals("2", out.get(0).messages.get(0).id);
    }

    @Test public void quotingPrefixesEveryLineSoAPastedReplyCannotBeConfusedWithTheOriginal() {
        String quoted = EmailRules.quoteBody("Jane <jane@example.com>", 1_700_000_000_000L, "line one\n\nline three");
        assertTrue(quoted, quoted.contains("Jane wrote:"));
        assertTrue(quoted, quoted.contains("> line one"));
        assertTrue(quoted, quoted.contains("> line three"));
        // The blank line is prefixed as well.
        assertTrue(quoted, quoted.contains("\n> \n"));
    }

    @Test public void theMailtoUriEncodesCharactersThatWouldTruncateIt() {
        // An unencoded ampersand or newline in a subject truncates the URI and silently drops
        // the rest of the message.
        String uri = EmailRules.mailtoUri(Arrays.asList("Jane <jane@example.com>"),
                "A & B", "line\nnext");
        assertTrue(uri, uri.startsWith("mailto:jane@example.com?"));
        assertTrue(uri, uri.contains("subject=A%20%26%20B"));
        assertTrue(uri, uri.contains("body=line%0Anext"));
        assertFalse(uri, uri.contains("&B"));
        String multi = EmailRules.mailtoUri(Arrays.asList("a@b.com", "c@d.com"), "s", "b");
        assertTrue(multi, multi.startsWith("mailto:a@b.com,c@d.com?"));
        // The at-sign must survive. Escaping it to %40 is permitted by RFC 6068 but a lot of
        // mail apps will not resolve it, and the failure surfaces as "no app can open this"
        // with nothing to indicate the address was the cause.
        assertFalse(uri, uri.contains("%40"));
        assertTrue(uri, uri.contains("jane@example.com"));
        // A space in a pasted address is still escaped rather than breaking the URI.
        assertTrue(EmailRules.mailtoUri(Arrays.asList("ja ne@b.com"), "", "").contains("%20"));
    }

    @Test public void foldersSeparateAndUnreadCountsOnlyTheGivenFolder() {
        List<EmailRules.Mail> all = Arrays.asList(
                new EmailRules.Mail("1", "", Collections.<String>emptyList(), "a", "", 1, "sent", true),
                new EmailRules.Mail("2", "", Collections.<String>emptyList(), "b", "", 2, "drafts", false),
                new EmailRules.Mail("3", "", Collections.<String>emptyList(), "c", "", 3, "drafts", true));
        assertEquals(2, EmailRules.inFolder(all, "drafts").size());
        assertEquals(1, EmailRules.inFolder(all, "sent").size());
        assertEquals(1, EmailRules.unreadCount(all, "drafts"));
        assertEquals(0, EmailRules.unreadCount(all, "sent"));
        // Newest first within a folder.
        assertEquals("3", EmailRules.inFolder(all, "drafts").get(0).id);
    }

    @Test public void sendabilityRequiresAValidRecipientAndABody() {
        assertTrue(EmailRules.isSendable(Arrays.asList("a@b.com"), "hello"));
        assertFalse(EmailRules.isSendable(Arrays.asList("a@b.com"), "   "));
        assertFalse(EmailRules.isSendable(Arrays.asList("nope"), "hello"));
        assertFalse(EmailRules.isSendable(Collections.<String>emptyList(), "hello"));
        assertFalse(EmailRules.isSendable(null, "hello"));
    }

    @Test public void anEmptySubjectAndBodyGetPlaceholders() {
        EmailRules.Mail mail = new EmailRules.Mail("1", "", null, "  ", null, 0, null, false);
        assertEquals("(no subject)", mail.subject);
        assertEquals("", mail.body);
        assertEquals(EmailRules.FOLDER_INBOX, mail.folder);
        assertTrue(mail.to.isEmpty());
    }

    @Test public void previewCollapsesWhitespaceAndTruncates() {
        assertEquals("a b", EmailRules.preview("  a\n\n b  "));
        assertEquals("", EmailRules.preview(null));
        StringBuilder long_ = new StringBuilder();
        for (int i = 0; i < 300; i++) long_.append('x');
        assertEquals(120, EmailRules.preview(long_.toString()).length());
    }

    @Test public void searchCoversSubjectBodyAndSender() {
        List<EmailRules.Mail> all = Arrays.asList(
                new EmailRules.Mail("1", "jane@x.com", null, "Lunch", "sandwich", 1, "sent", true),
                new EmailRules.Mail("2", "bob@x.com", null, "Invoice", "due", 2, "sent", true));
        assertEquals(1, EmailRules.filter(all, "SANDWICH").size());
        assertEquals(1, EmailRules.filter(all, "invoice").size());
        assertEquals(1, EmailRules.filter(all, "bob@").size());
        assertEquals(2, EmailRules.filter(all, "").size());
    }

    private static EmailRules.Mail mail(String id, String subject, long when) {
        return new EmailRules.Mail(id, "x@y.com", Collections.singletonList("z@y.com"),
                subject, "body", when, "inbox", false);
    }
}
