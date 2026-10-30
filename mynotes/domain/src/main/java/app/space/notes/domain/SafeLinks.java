package app.space.notes.domain;
import java.net.URI;
import java.net.URISyntaxException;
/** Validation only: never resolves DNS, fetches a URL or launches another application. */
public final class SafeLinks {
    private SafeLinks(){}
    public static String requireWeb(String value){
        if(value==null || value.length()>4096)throw new IllegalArgumentException("Invalid web address");
        for(int i=0;i<value.length();i++)if(Character.isISOControl(value.charAt(i)) || Character.isWhitespace(value.charAt(i)))throw new IllegalArgumentException("Web addresses cannot contain whitespace or controls");
        try{URI uri=new URI(value);String scheme=uri.getScheme();
            if(!("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme)) || uri.getHost()==null || uri.getHost().isEmpty() || uri.getUserInfo()!=null || uri.getPort()>65535 || uri.getPort()<-1)
                throw new IllegalArgumentException("Use an http or https address without embedded credentials");
            return value;
        }catch(URISyntaxException bad){throw new IllegalArgumentException("Invalid web address");}
    }
}
