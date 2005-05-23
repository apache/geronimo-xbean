/**
 *
 * Copyright 2005 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.gbean.metadata.simple;

/**
 * @version $Revision$ $Date$
 */
public class LotsOfTypes {
    private byte b;
    private short s;
    private int i;
    private long l;
    private float f;
    private double d;
    private char c;
    private boolean bool;
    private String string;

    private byte[] bArray;
    private short[] sArray;
    private int[] iArray;
    private long[] lArray;
    private float[] fArray;
    private double[] dArray;
    private char[] cArray;
    private boolean[] boolArray;
    private String[] stringArray;

    public LotsOfTypes() {
    }

    public LotsOfTypes(byte b, short s, int i, long l, float f, double d, char c, boolean bool, String string) {
        this.b = b;
        this.s = s;
        this.i = i;
        this.l = l;
        this.f = f;
        this.d = d;
        this.c = c;
        this.bool = bool;
        this.string = string;
    }

    public LotsOfTypes(byte[] bArray, short[] sArray, int[] iArray, long[] lArray, float[] fArray, double[] dArray, char[] cArray, boolean[] boolArray, String[] stringArray) {
        this.bArray = bArray;
        this.sArray = sArray;
        this.iArray = iArray;
        this.lArray = lArray;
        this.fArray = fArray;
        this.dArray = dArray;
        this.cArray = cArray;
        this.boolArray = boolArray;
        this.stringArray = stringArray;
    }

    public LotsOfTypes(byte b, short s, int i, long l, float f, double d, char c, boolean bool, String string, byte[] bArray, short[] sArray, int[] iArray, long[] lArray, float[] fArray, double[] dArray, char[] cArray, boolean[] boolArray, String[] stringArray) {
        this.b = b;
        this.s = s;
        this.i = i;
        this.l = l;
        this.f = f;
        this.d = d;
        this.c = c;
        this.bool = bool;
        this.string = string;
        this.bArray = bArray;
        this.sArray = sArray;
        this.iArray = iArray;
        this.lArray = lArray;
        this.fArray = fArray;
        this.dArray = dArray;
        this.cArray = cArray;
        this.boolArray = boolArray;
        this.stringArray = stringArray;
    }

    public void allBasic(byte b, short s, int i, long l, float f, double d, char c, boolean bool, String string) {
    }

    public void allArray(byte[] bArray, short[] sArray, int[] iArray, long[] lArray, float[] fArray, double[] dArray, char[] cArray, boolean[] boolArray, String[] stringArray) {
    }

    public void all(byte b, short s, int i, long l, float f, double d, char c, boolean bool, String string, byte[] bArray, short[] sArray, int[] iArray, long[] lArray, float[] fArray, double[] dArray, char[] cArray, boolean[] boolArray, String[] stringArray) {
    }


    public byte getB() {
        return b;
    }

    public void setB(byte b) {
        this.b = b;
    }

    public short getS() {
        return s;
    }

    public void setS(short s) {
        this.s = s;
    }

    public int getI() {
        return i;
    }

    public void setI(int i) {
        this.i = i;
    }

    public long getL() {
        return l;
    }

    public void setL(long l) {
        this.l = l;
    }

    public float getF() {
        return f;
    }

    public void setF(float f) {
        this.f = f;
    }

    public double getD() {
        return d;
    }

    public void setD(double d) {
        this.d = d;
    }

    public char getC() {
        return c;
    }

    public void setC(char c) {
        this.c = c;
    }

    public boolean isBool() {
        return bool;
    }

    public void setBool(boolean bool) {
        this.bool = bool;
    }

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }

    public byte[] getbArray() {
        return bArray;
    }

    public void setbArray(byte[] bArray) {
        this.bArray = bArray;
    }

    public short[] getsArray() {
        return sArray;
    }

    public void setsArray(short[] sArray) {
        this.sArray = sArray;
    }

    public int[] getiArray() {
        return iArray;
    }

    public void setiArray(int[] iArray) {
        this.iArray = iArray;
    }

    public long[] getlArray() {
        return lArray;
    }

    public void setlArray(long[] lArray) {
        this.lArray = lArray;
    }

    public float[] getfArray() {
        return fArray;
    }

    public void setfArray(float[] fArray) {
        this.fArray = fArray;
    }

    public double[] getdArray() {
        return dArray;
    }

    public void setdArray(double[] dArray) {
        this.dArray = dArray;
    }

    public char[] getcArray() {
        return cArray;
    }

    public void setcArray(char[] cArray) {
        this.cArray = cArray;
    }

    public boolean[] getBoolArray() {
        return boolArray;
    }

    public void setBoolArray(boolean[] boolArray) {
        this.boolArray = boolArray;
    }

    public String[] getStringArray() {
        return stringArray;
    }

    public void setStringArray(String[] stringArray) {
        this.stringArray = stringArray;
    }
}
