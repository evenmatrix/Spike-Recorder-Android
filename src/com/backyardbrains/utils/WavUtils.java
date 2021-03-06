package com.backyardbrains.utils;

import android.media.AudioFormat;
import android.support.annotation.NonNull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class WavUtils {

    private static final String RIFF_HEADER = "RIFF";
    private static final String WAVE_HEADER = "WAVE";
    private static final String FMT_HEADER = "fmt ";
    private static final String DATA_HEADER = "data";

    public static final int HEADER_SIZE = 44;

    private static final String CHARSET = "ASCII";

    /**
     * Converts specified {@code sampleCount} to wav time progress and returns it formatted as {@code mm:ss}.
     */
    public static CharSequence formatWavProgress(int sampleCount) {
        long byteCount = AudioUtils.getByteCount(sampleCount);
        byteCount -= HEADER_SIZE;

        return Formats.formatTime_mm_ss(TimeUnit.SECONDS.toMillis(getSeconds(byteCount)));
    }

    /**
     * Returns length of the wav file of specified {@code byteCount} length formatted as "XX s" or "XX m XX s".
     */
    public static CharSequence formatWavLength(long byteCount) {
        byteCount -= HEADER_SIZE;

        return Formats.formatTime_m_s(getSeconds(byteCount));
    }

    // Converts specified byteCount to seconds
    private static long getSeconds(long byteCount) {
        return byteCount / (AudioUtils.SAMPLE_RATE * 2);
    }

    public static byte[] getWavHeaderBytes(long totalAudioLength, int sampleRateInHz, int channelConfig,
        int audioFormat) {
        final int channels = (channelConfig == AudioFormat.CHANNEL_IN_MONO ? 1 : 2);
        final byte bitsPerSample;
        if (audioFormat == AudioFormat.ENCODING_PCM_8BIT) {
            bitsPerSample = 8;
        } else {
            bitsPerSample = 16;
        }

        return writeHeader(totalAudioLength - HEADER_SIZE, totalAudioLength - HEADER_SIZE + 36, sampleRateInHz,
            channels, bitsPerSample * sampleRateInHz * channels / 8, bitsPerSample);
    }

    private static byte[] writeHeader(long totalAudioLen, long totalDataLen, long longSampleRate, int channels,
        long byteRate, byte bitsPerSample) {

        byte[] header = new byte[HEADER_SIZE];

        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (channels * (bitsPerSample / 8)); //
        // block align
        header[33] = 0;
        header[34] = bitsPerSample; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        return header;
    }

    public static byte[] readWavPcm(@NonNull InputStream stream) throws IOException {
        WavInfo info = readHeader(stream);

        return readWavPcm(info, stream);
    }

    private static byte[] readWavPcm(@NonNull WavInfo info, @NonNull InputStream stream) throws IOException {
        byte[] data = new byte[info.getDataSize()];
        //noinspection ResultOfMethodCallIgnored
        stream.read(data, 0, data.length);

        return data;
    }

    /**
     * Reads a header of the specified {@code wavStream}.
     *
     * @throws IOException if specified stream is not a WAV stream
     */
    public static WavInfo readHeader(@NonNull InputStream wavStream) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        //noinspection ResultOfMethodCallIgnored
        wavStream.read(buffer.array(), buffer.arrayOffset(), buffer.capacity());

        buffer.rewind();
        buffer.position(buffer.position() + 20);

        int format = buffer.getShort();

        check(format == 1, "Unsupported encoding: " + format); // 1 means
        // Linear
        // PCM
        int channels = buffer.getShort();

        check(channels == 1 || channels == 2, "Unsupported channels: " + channels);

        int rate = buffer.getInt();

        check(rate <= 48000 && rate >= 11025, "Unsupported rate: " + rate);

        buffer.position(buffer.position() + 6);

        int bits = buffer.getShort();
        //check(bits == 16, "Unsupported bits: " + bits);

        int dataSize = 0;

        while (buffer.getInt() != 0x61746164) { // "data" marker
            int size = buffer.getInt();
            //noinspection ResultOfMethodCallIgnored
            wavStream.skip(size);

            buffer.rewind();
            //noinspection ResultOfMethodCallIgnored
            wavStream.read(buffer.array(), buffer.arrayOffset(), 8);
            buffer.rewind();
        }

        dataSize = buffer.getInt();

        check(dataSize > 0, "wrong data size: " + dataSize);

        return new WavInfo(rate, bits, channels == 2, dataSize);
    }

    // Convenience method that throws IOException with specified message if assertion is false
    private static void check(boolean assertion, String message) throws IOException {
        if (!assertion) throw new IOException(message);
    }

    /**
     * VO that holds information for a single WAV file.
     */
    public static class WavInfo {
        private final int sampleRate;
        private final int bits;
        private final int dataSize;

        boolean isStereo;

        WavInfo(int rate, int bits, boolean isStereo, int dataSize) {
            this.sampleRate = rate;
            this.bits = bits;
            this.isStereo = isStereo;
            this.dataSize = dataSize;
        }

        public int getSampleRate() {
            return sampleRate;
        }

        public int getBits() {
            return bits;
        }

        public int getDataSize() {
            return dataSize;
        }

        public boolean isStereo() {
            return isStereo;
        }
    }
}
