/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api;

import com.google.common.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;

/**
 * An enumeration of Java versions.
 */
public enum JavaVersion {
    VERSION_1_1, VERSION_1_2, VERSION_1_3, VERSION_1_4,
    VERSION_1_5, VERSION_1_6, VERSION_1_7, VERSION_1_8,
    VERSION_1_9, VERSION_1_10, VERSION_11, VERSION_UNKNOWN;
    // Before 9: http://www.oracle.com/technetwork/java/javase/versioning-naming-139433.html
    // 9+: http://openjdk.java.net/jeps/223
    private static final int FIRST_MAJOR_VERSION_ORDINAL = 8;
    private static JavaVersion currentJavaVersion;
    private final String versionName;

    JavaVersion() {
        this.versionName = ordinal() >= FIRST_MAJOR_VERSION_ORDINAL ? getMajorVersion() : "1." + getMajorVersion();
    }

    /**
     * Converts the given object into a {@code JavaVersion}.
     *
     * @param value An object whose toString() value is to be converted. May be null.
     * @return The version, or null if the provided value is null.
     * @throws IllegalArgumentException when the provided value cannot be converted.
     */
    public static JavaVersion toVersion(Object value) throws IllegalArgumentException {
        if (value == null) {
            return null;
        }
        if (value instanceof JavaVersion) {
            return (JavaVersion) value;
        }

        String name = value.toString();

        int firstNonVersionCharIndex = findFirstNonVersionCharIndex(name);

        String[] versionStrings = name.substring(0, firstNonVersionCharIndex).split("\\.");
        List<Integer> versions = convertToNumber(name, versionStrings);

        if (isLegacyVersion(versions)) {
            assertTrue(name, versions.get(1) > 0);
            return getVersionForMajor(versions.get(1));
        } else {
            return getVersionForMajor(versions.get(0));
        }
    }

    /**
     * Returns the version of the current JVM.
     *
     * @return The version of the current JVM.
     */
    public static JavaVersion current() {
        if (currentJavaVersion == null) {
            currentJavaVersion = toVersion(System.getProperty("java.version"));
        }
        return currentJavaVersion;
    }

    @VisibleForTesting
    static void resetCurrent() {
        currentJavaVersion = null;
    }

    public static JavaVersion forClassVersion(int classVersion) {
        return getVersionForMajor(classVersion - 44); //class file versions: 1.1 == 45, 1.2 == 46...
    }

    public static JavaVersion forClass(byte[] classData) {
        if (classData.length < 8) {
            throw new IllegalArgumentException("Invalid class format. Should contain at least 8 bytes");
        }
        return forClassVersion(classData[7] & 0xFF);
    }

    public boolean isJava5() {
        return this == VERSION_1_5;
    }

    public boolean isJava6() {
        return this == VERSION_1_6;
    }

    public boolean isJava7() {
        return this == VERSION_1_7;
    }

    public boolean isJava8() {
        return this == VERSION_1_8;
    }

    public boolean isJava9() {
        return this == VERSION_1_9;
    }

    public boolean isJava10() {
        return this == VERSION_1_10;
    }

    public boolean isJava5Compatible() {
        return this.compareTo(VERSION_1_5) >= 0;
    }

    public boolean isJava6Compatible() {
        return this.compareTo(VERSION_1_6) >= 0;
    }

    public boolean isJava7Compatible() {
        return this.compareTo(VERSION_1_7) >= 0;
    }

    public boolean isJava8Compatible() {
        return this.compareTo(VERSION_1_8) >= 0;
    }

    public boolean isJava9Compatible() {
        return this.compareTo(VERSION_1_9) >= 0;
    }

    @Incubating
    public boolean isJava10Compatible() {
        return this.compareTo(VERSION_1_10) >= 0;
    }

    @Override
    public String toString() {
        return getName();
    }

    private String getName() {
        return versionName;
    }

    public String getMajorVersion() {
        return String.valueOf(ordinal() + 1);
    }

    private static JavaVersion getVersionForMajor(int major) {
        return major >= values().length ? JavaVersion.VERSION_UNKNOWN : values()[major - 1];
    }

    private static void assertTrue(String value, boolean condition) {
        if (!condition) {
            throw new IllegalArgumentException("Could not determine java version from '" + value + "'.");
        }
    }

    private static boolean isLegacyVersion(List<Integer> versions) {
        return 1 == versions.get(0) && versions.size() > 1;
    }

    private static List<Integer> convertToNumber(String value, String[] versionStrs) {
        List<Integer> result = new ArrayList<Integer>();
        for (String s : versionStrs) {
            assertTrue(value, !isNumberStartingWithZero(s));
            try {
                result.add(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                assertTrue(value, false);
            }
        }
        assertTrue(value, !result.isEmpty() && result.get(0) > 0);
        return result;
    }

    private static boolean isNumberStartingWithZero(String number) {
        return number.length() > 1 && number.startsWith("0");
    }

    private static int findFirstNonVersionCharIndex(String s) {
        assertTrue(s, s.length() != 0);

        for (int i = 0; i < s.length(); ++i) {
            if (!isDigitOrPeriod(s.charAt(i))) {
                assertTrue(s, i != 0);
                return i;
            }
        }

        return s.length();
    }

    private static boolean isDigitOrPeriod(char c) {
        return (c >= '0' && c <= '9') || c == '.';
    }
}
