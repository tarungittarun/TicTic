package app.space.notes.domain;
/** Single real directory component, never an absolute path or provider document ID. */
public final class FolderNames {
 private FolderNames(){}
 public static String require(String name){
  if(name==null)throw new IllegalArgumentException("Enter a folder name");
  String value=name.trim();if(value.isEmpty() || value.length()>64 || value.equals(".") || value.equals("..") || value.equalsIgnoreCase(".mynotes-media-v1") || value.startsWith("mynotes-gen-"))throw new IllegalArgumentException("Use 1–64 characters for the folder name");
  for(int i=0;i<value.length();i++){char c=value.charAt(i);if(c=='/' || c=='\\' || Character.isISOControl(c))throw new IllegalArgumentException("Folder names cannot contain slashes or control characters");}
  return value;
 }
}
