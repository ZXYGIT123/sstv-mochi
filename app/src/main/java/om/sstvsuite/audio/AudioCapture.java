package om.sstvsuite.audio;

import android.media.AudioRecord;

/* loaded from: /home/Gold/Documents/deepseek-harness/default-workspace/sstv/apk/classes7.dex */
public final class AudioCapture {
    public static final int SAMPLE_RATE = 44100;
    private AudioRecord record;
    private volatile boolean running;
    private Thread thread;

    public interface Sink {
        void onFailure(AudioCapture audioCapture, Throwable th);

        void onSamples(AudioCapture audioCapture, short[] sArr, int i);
    }

    public boolean start(final Sink sink) {
        if (this.running) {
            return true;
        }
        int i = SAMPLE_RATE;
        int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, 16, 2);
        if (minBufferSize > 0) {
            i = minBufferSize;
        }
        try {
            AudioRecord audioRecord = new AudioRecord(1, SAMPLE_RATE, 16, 2, Math.max(i * 2, 8192));
            this.record = audioRecord;
            if (audioRecord.getState() != 1) {
                try {
                    this.record.release();
                } catch (Throwable unused) {
                }
                this.record = null;
                safeFailure(sink, new IllegalStateException("AudioRecord not initialized"));
                return false;
            }
            this.running = true;
            Thread thread = new Thread(new Runnable(this) { // from class: om.sstvsuite.audio.AudioCapture.1
                final /* synthetic */ AudioCapture this$0;

                {
                    this.this$0 = this;
                }

                @Override // java.lang.Runnable
                public void run() {
                    short[] sArr = new short[2048];
                    AudioRecord audioRecord2 = this.this$0.record;
                    if (audioRecord2 == null) {
                        return;
                    }
                    try {
                        audioRecord2.startRecording();
                        while (true) {
                            if (!this.this$0.running) {
                                break;
                            }
                            try {
                                int read = audioRecord2.read(sArr, 0, 2048);
                                if (!this.this$0.running) {
                                    break;
                                }
                                if (read > 0) {
                                    try {
                                        sink.onSamples(this.this$0, sArr, read);
                                    } catch (Throwable th) {
                                        if (this.this$0.running) {
                                            this.this$0.safeFailure(sink, th);
                                        }
                                    }
                                } else if (read < 0) {
                                    if (this.this$0.running) {
                                        this.this$0.safeFailure(sink, new IllegalStateException("AudioRecord read error: " + read));
                                    }
                                }
                            } catch (Throwable th2) {
                                if (this.this$0.running) {
                                    this.this$0.safeFailure(sink, th2);
                                }
                            }
                        }
                        this.this$0.running = false;
                        try {
                            audioRecord2.stop();
                        } catch (Throwable unused2) {
                        }
                    } catch (Throwable th3) {
                        this.this$0.running = false;
                        this.this$0.safeFailure(sink, th3);
                    }
                }
            }, "sstv-audio-capture");
            this.thread = thread;
            thread.start();
            return true;
        } catch (Throwable th) {
            this.record = null;
            safeFailure(sink, th);
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void safeFailure(Sink sink, Throwable th) {
        if (sink == null) {
            return;
        }
        try {
            sink.onFailure(this, th);
        } catch (Throwable unused) {
        }
    }

    public void stop() {
        this.running = false;
        AudioRecord audioRecord = this.record;
        this.record = null;
        if (audioRecord != null) {
            try {
                audioRecord.stop();
            } catch (Throwable unused) {
            }
        }
        Thread thread = this.thread;
        this.thread = null;
        if (thread != null) {
            try {
                thread.join(600L);
            } catch (InterruptedException unused2) {
                Thread.currentThread().interrupt();
            }
        }
        if (audioRecord != null) {
            try {
                audioRecord.release();
            } catch (Throwable unused3) {
            }
        }
    }

    public boolean isRunning() {
        return this.running;
    }
}
