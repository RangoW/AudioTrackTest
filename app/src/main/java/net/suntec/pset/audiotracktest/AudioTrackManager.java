package net.suntec.pset.audiotracktest;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.util.Log;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static android.media.AudioManager.AUDIO_SESSION_ID_GENERATE;

/**
 * Created by wangzhanfei on 17-12-1.
 */

public class AudioTrackManager {
    public static final String TAG = "AudioTrackManager";
    private AudioTrack mAudioTrack;
    private DataInputStream dis;
    private Thread recordThread;
    private Thread readThread;
    private boolean isStart = false;
    private static AudioTrackManager mInstance;
    private int mAudioMinBufSize;

    public AudioTrackManager() {
        mAudioMinBufSize = AudioTrack.getMinBufferSize(22050, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        Log.d(TAG, "AudioTrackManager: minbuff size " + mAudioMinBufSize);
        mAudioTrack = new AudioTrack(
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(22050)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                mAudioMinBufSize * 2,
                AudioTrack.MODE_STREAM,
                AUDIO_SESSION_ID_GENERATE
        );
    }

    /**
     * 获取单例引用
     *
     * @return AudioTrackManager Instance.
     */
    public static AudioTrackManager getInstance() {
        if (mInstance == null) {
            synchronized (AudioTrackManager.class) {
                if (mInstance == null) {
                    mInstance = new AudioTrackManager();
                }
            }
        }
        return mInstance;
    }

    /**
     * 销毁线程方法
     */
    private void destroyThread() {
        try {
            isStart = false;
            if (null != recordThread && Thread.State.RUNNABLE == recordThread.getState()) {
                try {
                    Thread.sleep(500);
                    recordThread.interrupt();
                } catch (Exception e) {
                    recordThread = null;
                }
            }
            recordThread = null;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            recordThread = null;
        }
    }

    /**
     * 启动播放线程
     */
    private void startThread() {
        destroyThread();
        isStart = true;
        if (recordThread == null) {
            recordThread = new Thread(recordRunnable);
            readThread = new Thread(readRunnable);
            recordThread.start();
            readThread.start();
        }
    }
    /**
     * 讀取線程
     */
    private Queue<byte[]> queue = new ConcurrentLinkedQueue<byte[]>();
    private Runnable readRunnable = new Runnable() {
        @Override
        public void run() {
            for (;;) {
                byte[] buffer = new byte[mAudioMinBufSize];
                try {
                    int readCount = dis.read(buffer, 0, mAudioMinBufSize);
                    if (readCount <=0 ) {
                        break;
                    }
                    queue.offer(buffer);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    /**
     * 播放线程
     */
    private Runnable recordRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
                byte[] tempBuffer = new byte[mAudioMinBufSize];
                short[] shortBuffer = new short[(mAudioMinBufSize+1)/2];
                int readCount = 0;
                short readValue;
                Log.d(TAG, "run: " + dis.available());
                mAudioTrack.play();
                while (dis.available() > 0) {
                    readCount = dis.read(tempBuffer, 0, mAudioMinBufSize);
                    Log.d(TAG, "read buffer: " + readCount);
                    if (readCount == AudioTrack.ERROR_INVALID_OPERATION || readCount == AudioTrack.ERROR_BAD_VALUE) {
                        continue;
                    }
                    if (readCount != 0 && readCount != -1) {
                        toBigdianShort(tempBuffer, shortBuffer);
                        Log.e(TAG, "short buffer: " + shortBuffer.length);
                        Log.d(TAG, "write buffer: " + mAudioTrack.write(shortBuffer, 0, shortBuffer.length, AudioTrack.WRITE_BLOCKING));
                    }
                    else {

                    }
                    readCount = 0;
                }


//                int readCount = 1;
//                int offset = 0;
////                mAudioTrack.play();
//                while(true) {
//                    short[] tmpBuffer = new short[mAudioMinBufSize];
//                    Log.d(TAG, "run: "+ (readCount = read_Short(tmpBuffer)));
//                    if (readCount == AudioTrack.ERROR_INVALID_OPERATION || readCount == AudioTrack.ERROR_BAD_VALUE) {
//                        Log.e(TAG, "run: write break!");
//                        break;
//                    }
//                    if (readCount > 0) {
//                        Log.d(TAG, "write: " + mAudioTrack.write(tmpBuffer, 0, readCount , AudioTrack.WRITE_BLOCKING));
////                        offset += mAudioMinBufSize;
//                    }
//                    else {
////                        mAudioTrack.stop();
////                        mAudioTrack.release();
//                        break;
//                    }
//                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    };
    private int toBigdianShort(byte[] b_buffer, short[] s_buffer) {
        for (int i=0; (i*2+1) <= b_buffer.length; i+=2) {
            if (i*2+1 == b_buffer.length ) {
                s_buffer[i] = (short)( b_buffer[i*2] & 0xff );
                break;
            }
            s_buffer[i] = (short)(( b_buffer[i*2+1] << 8 )|( b_buffer[i*2] & 0xFF ));
        }
        return 0;
    }

    private int read_Short(short[] buffer) throws IOException{
        int i = 0;
        while (i < mAudioMinBufSize && dis.available() > 0) {
            buffer[i] = dis.readShort();
            i++;
        }
        return i;
    }
    /**
     * 播放文件
     *
     * @param filepath pcm filepath
     * @throws Exception FileNotFound or IOException
     */
    private void setPath(String filepath) throws Exception {
//        File file = new File(path);
        dis = new DataInputStream(new FileInputStream(filepath));
    }

    /**
     * 启动
     * *
     * @param path: pcm path
     */
    public void start(String path) {
        try {
            setPath(path);
            startThread();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 播放
     * *
     * @return boolean
     */
    public boolean play() {
        Log.d(TAG, "play Channel Count: " + mAudioTrack.getChannelCount());
        Log.d(TAG, "play: SampleRate: " + mAudioTrack.getSampleRate());
        Log.d(TAG, "play: buffer size in Frames: " + mAudioTrack.getBufferSizeInFrames());
        Log.d(TAG, "play: Rate: " + mAudioTrack.getPlaybackRate());

        switch (mAudioTrack.getPlayState()) {
            case AudioTrack.PLAYSTATE_PAUSED:
            case AudioTrack.PLAYSTATE_STOPPED:
                mAudioTrack.play();
                break;
            case AudioTrack.PLAYSTATE_PLAYING:
                // already playing
                return false;
        }
        return true;
    }
    /**
     * 暂停
     *
     */
    public boolean pause() {
        if (mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
            mAudioTrack.pause();
        }
        else {
            // already Paused
            return false;
        }
        return true;
    }


    /**
     * 停止播放
     */
    public void stopPlay() {
        try {
            destroyThread();
            if (mAudioTrack != null) {
                if (mAudioTrack.getState() == AudioRecord.STATE_INITIALIZED) {
                    mAudioTrack.stop();
                }
                if (mAudioTrack != null) {
                    mAudioTrack.release();
                }
            }
            if (dis != null) {
                dis.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}