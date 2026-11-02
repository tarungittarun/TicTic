#include <jni.h>
#include <atomic>
#include <memory>
#include <string>
#include <vector>
#include "whisper.h"

struct Session {whisper_context* context=nullptr;std::atomic<bool> cancelled{false};~Session(){if(context)whisper_free(context);}};
static void fail(JNIEnv* env,const char* message){env->ThrowNew(env->FindClass("java/io/IOException"),message);}
static bool abort_decode(void* data){return static_cast<Session*>(data)->cancelled.load();}
extern "C" JNIEXPORT jlong JNICALL Java_app_space_notes_runtime_TrainedWhisper_create(JNIEnv* env,jclass,jstring path){
 try{
  const char* value=env->GetStringUTFChars(path,nullptr);if(!value)return 0;
  std::string file(value);env->ReleaseStringUTFChars(path,value);
  // No audio, transcript, private paths or debug tensors in native logs.
  whisper_log_set([](enum ggml_log_level,const char*,void*){},nullptr);
  auto session=std::make_unique<Session>();auto params=whisper_context_default_params();params.use_gpu=false;params.flash_attn=false;
  session->context=whisper_init_from_file_with_params(file.c_str(),params);
  if(!session->context){fail(env,"Experimental offline model could not be loaded; original audio retained");return 0;}
  return reinterpret_cast<jlong>(session.release());
 }catch(...){fail(env,"Not enough resources to load experimental speech; original audio retained");return 0;}
}
extern "C" JNIEXPORT void JNICALL Java_app_space_notes_runtime_TrainedWhisper_cancel(JNIEnv*,jclass,jlong handle){if(handle)reinterpret_cast<Session*>(handle)->cancelled.store(true);}
extern "C" JNIEXPORT void JNICALL Java_app_space_notes_runtime_TrainedWhisper_release(JNIEnv*,jclass,jlong handle){delete reinterpret_cast<Session*>(handle);}
extern "C" JNIEXPORT jbyteArray JNICALL Java_app_space_notes_runtime_TrainedWhisper_decode(JNIEnv* env,jclass,jlong handle,jfloatArray input,jint count){
 auto session=reinterpret_cast<Session*>(handle);
 if(!session || count<=0 || count>480000 || count>env->GetArrayLength(input)){fail(env,"Invalid bounded speech window; original audio retained");return nullptr;}
 try{
  std::vector<float> pcm(count);env->GetFloatArrayRegion(input,0,count,pcm.data());if(env->ExceptionCheck())return nullptr;
  auto p=whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
  p.n_threads=2;p.translate=false;p.language="en";p.detect_language=false;p.no_context=true;p.no_timestamps=true;
  p.print_progress=false;p.print_realtime=false;p.print_timestamps=false;p.print_special=false;
  p.temperature=0;p.temperature_inc=0;p.greedy.best_of=1;p.max_tokens=128;
  p.abort_callback=abort_decode;p.abort_callback_user_data=session;
  if(whisper_full(session->context,p,pcm.data(),count)!=0){fail(env,"Experimental recognition stopped; original audio retained");return nullptr;}
  std::string text;
  for(int i=0;i<whisper_full_n_segments(session->context);i++){
   if(whisper_full_get_segment_no_speech_prob(session->context,i)>.6f)continue;
   text+=whisper_full_get_segment_text(session->context,i);
  }
  auto result=env->NewByteArray(text.size());if(result)env->SetByteArrayRegion(result,0,text.size(),reinterpret_cast<const jbyte*>(text.data()));return result;
 }catch(...){fail(env,"Experimental recognition could not finish; original audio retained");return nullptr;}
}
