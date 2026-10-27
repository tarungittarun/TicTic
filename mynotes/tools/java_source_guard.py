"""Small Java-8 literal/comment sanity guard, not a substitute for javac."""
def check_literals(text):
    state='code';i=0;line=1
    while i<len(text):
        c=text[i];n=text[i+1] if i+1<len(text) else ''
        if state in ('string','char'):
            if c in '\r\n':raise ValueError(f'Raw newline in {state} literal at line {line}')
            if c=='\\':
                if n in ('\n','\r',''):raise ValueError(f'Invalid escape at line {line}')
                i+=2;continue
            if c==('"' if state=='string' else "'"):state='code'
        elif state=='line':
            if c=='\n':state='code'
        elif state=='block':
            if c=='*' and n=='/':state='code';i+=2;continue
        else:
            if c=='/' and n=='/':state='line';i+=2;continue
            if c=='/' and n=='*':state='block';i+=2;continue
            if c=='"':state='string'
            if c=="'":state='char'
        if c=='\n':line+=1
        i+=1
    if state not in ('code','line'):raise ValueError('Unterminated '+state)
