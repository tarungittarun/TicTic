package app.space.notes.domain;
import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.IOException;
import static org.junit.Assert.*;
public class JsonLimitsTest {
    @Test public void escapedQuotesAndBracketTextDoNotIncreaseNesting()throws Exception{JsonLimits.require("{\"text\":\"[[[ \\\" {{{\",\"list\":[{}]}".getBytes(StandardCharsets.UTF_8));}
    @Test public void deeplyNestedInputFailsWithoutRecursion(){String input=String.join("",Collections.nCopies(1000,"["))+"0"+String.join("",Collections.nCopies(1000,"]"));assertThrows(IOException.class,()->JsonLimits.require(input.getBytes(StandardCharsets.UTF_8)));}
    @Test public void unbalancedAndWideStructuresAreRefused(){for(String value:Arrays.asList("{]","[","\"unclosed"))assertThrows(IOException.class,()->JsonLimits.require(value.getBytes(StandardCharsets.UTF_8)));String wide="["+String.join(",",Collections.nCopies(JsonLimits.MAX_STRUCTURES,"{}"))+"]";assertThrows(IOException.class,()->JsonLimits.require(wide.getBytes(StandardCharsets.UTF_8)));}
    @Test public void malformedUtf8CannotBeSilentlyReplacedBeforeParsing()throws Exception{String text="{\"text\":\"😀 ಕನ್ನಡ\"}";assertEquals(text,JsonLimits.text(text.getBytes(StandardCharsets.UTF_8)));assertThrows(IOException.class,()->JsonLimits.text(new byte[]{'{','\"',(byte)0xc3,0x28,'\"',':','0','}'}));}

}
