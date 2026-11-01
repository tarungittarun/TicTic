package app.space.notes.domain;
import java.util.*;
import java.util.regex.*;
/** Small offline extractive ranker. Selects source sentences, never invents facts or calls a model/service. */
public final class LocalSummary {
 private LocalSummary(){}
 private static final Pattern WORD=Pattern.compile("[\\p{L}\\p{N}]{2,}");
 private static final Set<String> STOP=new HashSet<>(Arrays.asList("the","and","that","this","with","from","have","will","are","was","were","for","but","not","you","your","our","into","can","has","had","its","they","their"));
 public static List<String> extract(String source,int maximum){
  if(source==null || source.length()>100000 || maximum<1 || maximum>5)throw new IllegalArgumentException("Summary accepts at most 100,000 characters and 1–5 sentences");RichText.requireText(source);
  List<String> sentences=new ArrayList<>();for(String part:source.split("(?<=[.!?।])\\s+|[\\r\\n]+")){String value=part.trim();if(value.isEmpty())continue;if(value.length()>1200)continue;if(sentences.size()==500)break;sentences.add(value);}
  Map<String,Integer> frequency=new HashMap<>();List<Set<String>> tokens=new ArrayList<>();for(String sentence:sentences){Set<String> words=new HashSet<>();Matcher matcher=WORD.matcher(sentence.toLowerCase(Locale.ROOT));while(matcher.find() && words.size()<128){String word=matcher.group();if(!STOP.contains(word))words.add(word);}tokens.add(words);for(String word:words)frequency.put(word,frequency.getOrDefault(word,0)+1);}
  List<Integer> order=new ArrayList<>();Map<Integer,Double> score=new HashMap<>();for(int i=0;i<sentences.size();i++){double value=0;for(String word:tokens.get(i))value+=Math.log(1+frequency.get(word));score.put(i,value/Math.sqrt(Math.max(1,tokens.get(i).size())));order.add(i);}order.sort((a,b)->{int c=Double.compare(score.get(b),score.get(a));return c==0?Integer.compare(a,b):c;});
  Set<String> seen=new HashSet<>();List<Integer> selected=new ArrayList<>();int characters=0;for(int index:order){String text=sentences.get(index);if(selected.size()==maximum)break;if(seen.add(text) && characters+text.length()<=2400){selected.add(index);characters+=text.length();}}Collections.sort(selected);List<String> result=new ArrayList<>();for(int index:selected)result.add(sentences.get(index));return Collections.unmodifiableList(result);
 }
}
