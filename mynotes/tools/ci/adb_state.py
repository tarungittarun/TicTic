"""Verify a root/unroot state transition, tolerating only transport unavailability while adbd restarts."""
import argparse
import subprocess
import time

def wait_uid(expected,seconds=20,run=subprocess.run,clock=time.monotonic,sleep=time.sleep):
    deadline=clock()+seconds;last='no response'
    while clock()<deadline:
        try:
            result=run(['adb','shell','id','-u'],capture_output=True,text=True,timeout=3)
            last=(result.stdout+' '+result.stderr).strip()
            if result.returncode==0 and result.stdout.strip()==str(expected):return expected
        except subprocess.TimeoutExpired:last='adbd transition timed out'
        sleep(.2)
    raise RuntimeError(f'adbd did not reach required UID {expected}: {last[:500]}')

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--uid',type=int,choices=[0,2000],required=True);args=parser.parse_args();print('Verified shell UID:',wait_uid(args.uid))
