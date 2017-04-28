package com.shiny.baselibrary.utils;

import android.annotation.TargetApi;
import android.os.Build;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

// @formatter:off
/**
 * @author shiny.zeng
 * @Create at 2014-9-15 上午11:34:14
 * @Version 1.0
 * <p><strong>Features draft description.主要功能介绍</strong></p>
 * 计算文件CID
 */
// @formatter:on
public class CIDUtils {
    // ===========================================================
    // Constants
    // ===========================================================

    private static final int  PART_SIZE = 20 * 1024; // 默认每段20K
    private static final char hex[]     = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    // ===========================================================
    // Fields
    // ===========================================================

    // ===========================================================
    // Constructors
    // ===========================================================

    // ===========================================================
    // Getter & Setter
    // ===========================================================

    // ===========================================================
    // Methods for/from SuperClass/Interfaces
    // ===========================================================

    // ===========================================================
    // Methods
    // ===========================================================

    // 计算文件的CID
    public static String getCidHex(File file) {
        byte[] cidBytes = getCidBytes(file);
        if(cidBytes != null){
            return toHexString(cidBytes);
        }
        return "";
    }

    public static byte[] getCidBytes(final File file) {
        if (!file.exists() || !file.isFile()) { return null; }
        final long filesize = file.length();
        byte[] bytes =
            (filesize < 3 * PART_SIZE) ? readSmallerFile(file, (int) filesize) : readBiggerFile(file, filesize);
        return encryptStreamBytes(bytes);
    }

    // 计算ZIP文件中的CID（zip文件中有且只有一个文件）
    public static String getZipEntryCidHex(final File file) {
        byte[] cidBytes = getZipEntryCidBytes(file);
        if(cidBytes != null){
            return toHexString(cidBytes);
        }
        return "";
    }


    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static byte[] getZipEntryCidBytes(final File file) {
        long fileSize = 0;
        if (!file.exists() || !file.isFile() || !file.getName().endsWith("zip")) { return null; }
        ZipEntry ze = null;
        ZipFile zipFile = null;
        byte[] result = null;
        try {
            zipFile = new ZipFile(file);
            Enumeration<? extends ZipEntry> e = zipFile.entries();
            ze = e.nextElement();
            fileSize = ze.getSize();
            byte[] bytes = readZipFile(file, fileSize);
            result = encryptStreamBytes(bytes);
        } catch (ZipException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally{
            StreamCloseUtils.close(zipFile);
        }
        return result;
    }

    // 读取全文
    private static byte[] readSmallerFile(final File file, final int filesize) {
        byte[] bytes = new byte[filesize];
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            bytes = readSmallerStream(in, filesize);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            StreamCloseUtils.close(in);
        }
        return bytes;
    }

    // 读取前/中/后三段
    private static byte[] readBiggerFile(final File file, final long filesize) {
        byte[] bytes = new byte[PART_SIZE * 3];
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            bytes = readBiggerStream(in, filesize);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            StreamCloseUtils.close(in);
        }
        return bytes;
    }

    // 读取ZIP文件中前/中/后三段
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static byte[] readZipFile(final File file, final long filesize) {
        ZipEntry ze = null;
        ZipFile zipFile = null;
        InputStream in = null;
        byte[] bytes = new byte[PART_SIZE * 3];
        try {
            zipFile = new ZipFile(file);
            Enumeration<? extends ZipEntry> e = zipFile.entries();
            ze = e.nextElement();
            in = zipFile.getInputStream(ze);
            bytes = readBiggerStream(in, filesize);
        }  catch (Exception e) {
            e.printStackTrace();
        } finally {
            StreamCloseUtils.close(in);
            StreamCloseUtils.close(zipFile);
        }
        return bytes;
    }

    // 全文读取
    private static byte[] readSmallerStream(final InputStream in, final int streamlength) throws IOException {
        byte[] bytes = new byte[streamlength];
        int readed = in.read(bytes);
        int patchsize = 1;
        while (patchsize > 0 && readed != streamlength) {
            patchsize = in.read(bytes, readed, streamlength - readed);
            if (patchsize > 0) {
                readed += patchsize;
            }
        }
        if (readed != streamlength) { throw new IOException("InputStream(" + in + ", streamlength=" + streamlength
            + ", readedlength=" + readed + ") read fail"); }
        return bytes;
    }

    // 读取文件前/中/后三段
    private static byte[] readBiggerStream(final InputStream in, final long streamlength) throws IOException {
        byte[] bytes = new byte[PART_SIZE * 3];
        int readed = -1;
        int patchsize = -1;
        long skiped = -1;
        long patchskip = -1;
        long pointpos = 0;
        readed = in.read(bytes, 0, PART_SIZE);
        patchsize = readed;
        while (patchsize > 0 && readed != PART_SIZE) {
            patchsize = in.read(bytes, readed, PART_SIZE - readed);
            if (patchsize > 0) {
                readed += patchsize;
            }
        }
        pointpos = streamlength / 3 - PART_SIZE;
        skiped = in.skip(pointpos);
        patchskip = skiped;
        while (patchskip > 0 && skiped != pointpos) {
            patchskip = in.skip(pointpos - skiped);
            if (patchskip > 0) {
                skiped += patchskip;
            }
        }
        readed = in.read(bytes, PART_SIZE, PART_SIZE);
        patchsize = readed;
        while (patchsize > 0 && readed != PART_SIZE) {
            patchsize = in.read(bytes, PART_SIZE + readed, PART_SIZE - readed);
            if (patchsize > 0) {
                readed += patchsize;
            }
        }
        pointpos = streamlength - streamlength / 3 - PART_SIZE * 2;
        skiped = in.skip(pointpos);
        patchskip = skiped;
        while (patchskip > 0 && skiped != pointpos) {
            patchskip = in.skip(pointpos - skiped);
            if (patchskip > 0) {
                skiped += patchskip;
            }
        }
        readed = in.read(bytes, PART_SIZE * 2, PART_SIZE);
        patchsize = readed;
        while (patchsize > 0 && readed != PART_SIZE) {
            patchsize = in.read(bytes, PART_SIZE * 2 + readed, PART_SIZE - readed);
            if (patchsize > 0) {
                readed += patchsize;
            }
        }
        return bytes;
    }

    // 将字节数组进行加SHA-1密码
    static byte[] encryptStreamBytes(byte[] streambytes) {
        return createSHA1Digest().digest(streambytes);
    }

    private static MessageDigest createSHA1Digest() {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return digest;
    }

    // 将二进制转换成16位字符串
    public static String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(hex[((b >> 4) & 0xF)]).append(hex[((b >> 0) & 0xF)]);
        }
        return sb.toString();
    }

    public static void main(String[] args) throws Throwable {
        byte[] bytes = new byte[PART_SIZE * 3];
        System.out.println(toHexString(bytes));
    }

    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================
}
