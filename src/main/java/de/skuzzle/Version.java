/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Simon Taddiken
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.skuzzle;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is an implementation of the full <em>semantic version 2.0.0</em>
 * <a href="http://semver.org/">specification</a>. Instances can be obtained
 * using the static overloads of the <em>create</em> method or by
 * {@link #parseVersion(String) parsing} a String. This class implements
 * {@link Comparable} to compare two versions by following the specifications
 * linked to above. The {@link #equals(Object)} method conforms to the result of
 * {@link #compareTo(Version)}, {@link #hashCode()} is implemented
 * appropriately. Neither method considers the {@link #getBuildMetaData() build
 * meta data} field for comparison.
 *
 * @author Simon Taddiken
 */
public final class Version implements Comparable<Version>, Serializable {

    /** Conforms to Version implementation 0.1.0 */
    private static final long serialVersionUID = -7080189911455871050L;

    /** Semantic Version Specification to which this class complies */
    public static Version COMPLIANCE = Version.create(2, 0, 0);

    /**
     * This exception indicates that a version- or a part of a version string
     * could not be parsed according to the semantic version specification.
     *
     * @author Simon Taddiken
     */
    public static class VersionFormatException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public VersionFormatException(String message) {
            super(message);
        }
    }

    private final static Pattern PRE_RELEASE = Pattern.compile(
            "(?:(?:[0-9]+[a-zA-Z-][\\w-]*)|(?:[a-zA-Z][\\w-]*)|(?:[1-9]\\d*)|0)(?:\\.(?:(?:[0-9]+[a-zA-Z-][\\w-]*)|(?:[a-zA-Z][\\w-]*)|(?:[1-9]\\d*)|0))*");
    private final static Pattern BUILD_MD = Pattern.compile("[\\w-]+(\\.[\\w-]+)*");
    private final static Pattern VERSION_PATTERN = Pattern.compile(
            "(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:(?:[0-9]+[a-zA-Z-][\\w-]*)|(?:[a-zA-Z][\\w-]*)|(?:[1-9]\\d*)|0)(?:\\.(?:(?:[0-9]+[a-zA-Z-][\\w-]*)|(?:[a-zA-Z][\\w-]*)|(?:[1-9]\\d*)|0))*))?(?:\\+([\\w-]+(\\.[\\w-]+)*))?");

    // Match result group indices
    private final static int MAJOR_GROUP = 1;
    private final static int MINOR_GROUP = 2;
    private final static int PATCH_GROUP = 3;
    private final static int PRE_RELEASE_GROUP = 4;
    private final static int BUILD_MD_GROUP = 5;

    /**
     * Comparator for natural version ordering. See
     * {@link #compare(Version, Version)} for more information.
     */
    public final static Comparator<Version> NATURAL_ORDER = new Comparator<Version>() {
        @Override
        public int compare(Version o1, Version o2) {
            return Version.compare(o1, o2);
        }
    };

    /**
     * Compares two versions, following the <em>semantic versioning</em>
     * specification. Here is a quote from <a href="http://semver.org/">semantic
     * version 2.0.0 specification</a>:
     *
     * <p>
     * <em> Precedence refers to how versions are compared to each other when
     * ordered. Precedence MUST be calculated by separating the version into
     * major, minor, patch and pre-release identifiers in that order (Build
     * metadata does not figure into precedence). Precedence is determined by
     * the first difference when comparing each of these identifiers from left
     * to right as follows: Major, minor, and patch versions are always compared
     * numerically. Example: 1.0.0 &lt; 2.0.0 &lt; 2.1.0 &lt; 2.1.1. When major, minor,
     * and patch are equal, a pre-release version has lower precedence than a
     * normal version. Example: 1.0.0-alpha &lt; 1.0.0. Precedence for two
     * pre-release versions with the same major, minor, and patch version MUST
     * be determined by comparing each dot separated identifier from left to
     * right until a difference is found as follows: identifiers consisting of
     * only digits are compared numerically and identifiers with letters or
     * hyphens are compared lexically in ASCII sort order. Numeric identifiers
     * always have lower precedence than non-numeric identifiers. A larger set
     * of pre-release fields has a higher precedence than a smaller set, if all
     * of the preceding identifiers are equal. Example: 1.0.0-alpha &lt;
     * 1.0.0-alpha.1 &lt; 1.0.0-alpha.beta &lt; 1.0.0-beta &lt; 1.0.0-beta.2 &lt;
     * 1.0.0-beta.11 &lt; 1.0.0-rc.1 &lt; 1.0.0.
     * </em>
     * </p>
     *
     * <p>
     * This method fulfills the general contract for Java's {@link Comparator
     * Comparators} and {@link Comparable Comparables}.
     * </p>
     *
     * @param v1 The first version for comparison.
     * @param v2 The second version for comparison.
     * @return A value below 0 iff <tt>v1 &lt; v2</tt>, a value above 0 iff
     *         <tt>v1 &gt; v2</tt> and 0 iff <tt>v1 = v2</tt>.
     * @throws NullPointerException If either parameter is null.
     */
    public static int compare(Version v1, Version v2) {
        if (v1 == null) {
            throw new NullPointerException("v1 is null");
        } else if (v2 == null) {
            throw new NullPointerException("v2 is null");
        } else if (v1 == v2) {
            return 0;
        }

        int mc, mm, mp;
        if ((mc = Integer.compare(v1.major, v2.major)) == 0) {
            if ((mm = Integer.compare(v1.minor, v2.minor)) == 0) {
                if ((mp = Integer.compare(v1.patch, v2.patch)) == 0) {

                    if (!v1.isPreRelease() && !v2.isPreRelease()) {
                        // both are no pre releases
                        return 0;
                    } else if (v1.isPreRelease() && v2.isPreRelease()) {
                        final String[] thisParts = v1.getPreReleaseParts();
                        final String[] otherParts = v2.getPreReleaseParts();

                        int min = Math.min(thisParts.length, otherParts.length);
                        for (int i = 0; i < min; ++i) {
                            final int r = comparePreReleaseParts(thisParts[i],
                                    otherParts[i]);
                            if (r != 0) {
                                // versions differ in pre release part i
                                return r;
                            }
                        }

                        // all pre release id's are equal, so compare amount of
                        // pre release id's
                        return Integer.compare(thisParts.length, otherParts.length);

                    } else if (v1.isPreRelease()) {
                        // other is greater, because it is no pre release
                        return -1;
                    } else if (v2.isPreRelease()) {
                        // this is greater because it is no pre release
                        return 1;
                    }

                } else {
                    // versions differ in patch
                    return mp;
                }
            } else {
                // versions differ in minor
                return mm;
            }
        } else {
            // versions differ in major
            return mc;
        }
        return 0;
    }

    private static int comparePreReleaseParts(String p1, String p2) {
        final int num1 = isNumeric(p1);
        final int num2 = isNumeric(p2);

        if (num1 < 0 && num2 < 0) {
            // both are not numerical -> compare lexically
            return p1.compareTo(p2);
        } else if (num1 >= 0 && num2 >= 0) {
            // both are numerical
            return Integer.compare(num1, num2);
        } else if (num1 >= 0) {
            // only part1 is numerical -> p2 is greater
            return -1;
        } else if (num2 >= 0) {
            // only part2 is numerical -> p1 is greater
            return 1;
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * Determines whether s is a positive number. If it is, the number is
     * returned, otherwise the result is -1.
     *
     * @param s The String to check.
     * @return The positive number (incl. 0) if s a number, or -1 if it is not.
     */
    private static int isNumeric(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Creates a new Version from the provided components. Neither value of
     * <tt>major, minor</tt> or <tt>patch</tt> must be lower than 0 and at least
     * one must be greater than zero. <tt>preRelease</tt> or
     * <tt>buildMetaData</tt> may be the empty String. In this case, the created
     * <tt>Version</tt> will have no pre release resp. build meta data field. If
     * those parameters are not empty, they must conform to the semantic
     * versioning specification.
     *
     * @param major The major version.
     * @param minor The minor version.
     * @param patch The patch version.
     * @param preRelease The pre release version or the empty string.
     * @param buildMetaData The build meta data field or the empty string.
     * @return The version instance.
     * @throws VersionFormatException If <tt>preRelease</tt> or
     *             <tt>buildMetaData</tt> does not conform to the semantic
     *             versioning specification.
     */
    public final static Version create(int major, int minor, int patch,
            String preRelease,
            String buildMetaData) {
        checkParams(major, minor, patch);
        if (preRelease == null) {
            throw new IllegalArgumentException("preRelease is null");
        } else if (buildMetaData == null) {
            throw new IllegalArgumentException("buildMetaData is null");
        }
        if (!preRelease.isEmpty() && !PRE_RELEASE.matcher(preRelease).matches()) {
            throw new VersionFormatException(preRelease);
        }
        if (!buildMetaData.isEmpty() && !BUILD_MD.matcher(buildMetaData).matches()) {
            throw new VersionFormatException(buildMetaData);
        }
        return new Version(major, minor, patch, preRelease, buildMetaData);
    }

    /**
     * Creates a new Version from the provided components. The version's build
     * meta data field will be empty. Neither value of <tt>major, minor</tt> or
     * <tt>patch</tt> must be lower than 0 and at least one must be greater than
     * zero. <tt>preRelease</tt> may be the empty String. In this case, the
     * created version will have no pre release field. If it is not empty, it
     * must conform to the specifications of the semantic versioning.
     *
     * @param major The major version.
     * @param minor The minor version.
     * @param patch The patch version.
     * @param preRelease The pre release version or the empty string.
     * @return The version instance.
     * @throws VersionFormatException If <tt>preRelease</tt> is not empty and
     *             does not conform to the semantic versioning specification
     */
    public final static Version create(int major, int minor, int patch, String preRelease) {
        checkParams(major, minor, patch);
        if (preRelease == null) {
            throw new IllegalArgumentException("preRelease is null");
        }
        if (!PRE_RELEASE.matcher(preRelease).matches()) {
            throw new VersionFormatException(preRelease);
        }
        return new Version(major, minor, patch, preRelease, "");
    }

    /**
     * Creates a new Version from the three provided components. The version's
     * pre release and build meta data fields will be empty. Neither value must
     * be lower than 0 and at least one must be greater than zero
     *
     * @param major The major version.
     * @param minor The minor version.
     * @param patch The patch version.
     * @return The version instance.
     */
    public final static Version create(int major, int minor, int patch) {
        checkParams(major, minor, patch);
        return new Version(major, minor, patch, "", "");
    }

    private static void checkParams(int major, int minor, int patch) {
        if (major < 0) {
            throw new IllegalArgumentException("major < 0");
        } else if (minor < 0) {
            throw new IllegalArgumentException("minor < 0");
        } else if (patch < 0) {
            throw new IllegalArgumentException("patch < 0");
        } else if (major == 0 && minor == 0 && patch == 0) {
            throw new IllegalArgumentException("all parts are 0");
        }
    }

    /**
     * Tries to parse the provided String as a semantic version. If the string
     * does not conform to the semantic versioning specification, a
     * {@link VersionFormatException} will be thrown.
     *
     * @param versionString The String to parse.
     * @return The parsed version.
     * @throws VersionFormatException If the String is no valid version
     * @throws IllegalArgumentException If <tt>versionString</tt> is
     *             <code>null</code>.
     */
    public final static Version parseVersion(String versionString) {
        if (versionString == null) {
            throw new IllegalArgumentException("versionString is null");
        }
        final Matcher m = VERSION_PATTERN.matcher(versionString);
        if (!m.matches()) {
            throw new VersionFormatException(versionString);
        }

        final int major = Integer.parseInt(m.group(MAJOR_GROUP));
        final int minor = Integer.parseInt(m.group(MINOR_GROUP));
        final int patch = Integer.parseInt(m.group(PATCH_GROUP));

        checkParams(major, minor, patch);

        final String preRelease;
        if (m.group(PRE_RELEASE_GROUP) != null) {
            preRelease = m.group(PRE_RELEASE_GROUP);
        } else {
            preRelease = "";
        }

        final String buildMD;
        if (m.group(BUILD_MD_GROUP) != null) {
            buildMD = m.group(BUILD_MD_GROUP);
        } else {
            buildMD = "";
        }

        return new Version(major, minor, patch, preRelease, buildMD);
    }

    private final int major;
    private final int minor;
    private final int patch;
    private final String preRelease;
    private final String buildMetaData;

    private Version(int major, int minor, int patch, String preRelease, String buildMd) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.preRelease = preRelease;
        this.buildMetaData = buildMd;
    }

    /**
     * Gets this version's major number.
     *
     * @return The major version.
     */
    public int getMajor() {
        return this.major;
    }

    /**
     * Gets this version's minor number.
     *
     * @return The minor version.
     */
    public int getMinor() {
        return this.minor;
    }

    /**
     * Gets this version's path number.
     *
     * @return The patch number.
     */
    public int getPatch() {
        return this.patch;
    }

    /**
     * Gets the pre release parts of this version as array by splitting the pre
     * result version string at the dots.
     *
     * @return Pre release version as array. Array is empty if this version has
     *         no pre release part.
     */
    public String[] getPreReleaseParts() {
        return this.preRelease.split("\\.");
    }

    /**
     * Gets the pre release identifier of this version. If this version has no
     * such identifier, an empty string is returned.
     *
     * @return This version's pre release identifier or an empty String if this
     *         version has no such identifier.
     */
    public String getPreRelease() {
        return this.preRelease;
    }

    /**
     * Gets this version's build meta data. If this version has no build meta
     * data, the returned string is empty.
     *
     * @return The build meta data or an empty String if this version has no
     *         build meta data.
     */
    public String getBuildMetaData() {
        return this.buildMetaData;
    }

    /**
     * Gets this version's build meta data as array by splitting the meta data
     * at dots. If this version has no build meta data, the result is an empty
     * array.
     *
     * @return The build meta data as array.
     */
    public String[] getBuildMetaDataParts() {
        return this.buildMetaData.split("\\.");
    }

    /**
     * Determines whether this version is still under initial development.
     *
     * @return <code>true</code> iff this version's major part is zero.
     */
    public boolean isInitialDevelopment() {
        return this.major == 0;
    }

    /**
     * Determines whether this is a pre release version.
     *
     * @return <code>true</code> iff {@link #getPreRelease()} is not empty.
     */
    public boolean isPreRelease() {
        return !this.preRelease.isEmpty();
    }

    /**
     * Creates a String representation of this version by joining its parts
     * together as by the semantic version specification.
     *
     * @return The version as a String.
     */
    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder(this.preRelease.length()
                + this.buildMetaData.length() + 24);
        b.append(this.major).append(".").append(this.minor).append(".")
                .append(this.patch);
        if (!this.preRelease.isEmpty()) {
            b.append("-").append(this.preRelease);
        }
        if (!this.buildMetaData.isEmpty()) {
            b.append("+").append(this.buildMetaData);
        }
        return b.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.major, this.minor, this.patch, this.preRelease);
    }

    /**
     * Determines whether this version is equal to the passed object. This is
     * the case if the passed object is an instance of Version and this version
     * {@link #compareTo(Version) compared} to the provided one yields 0. Thus,
     * this method ignores the {@link #getBuildMetaData()} field.
     *
     * @param obj the object to compare to.
     * @return <code>true</code> iff <tt>obj</tt> is an instance of
     *         <tt>Version</tt> and <tt>this.compareTo((Version) obj) == 0</tt>
     * @see #compareTo(Version)
     */
    @Override
    public boolean equals(Object obj) {
        return obj == this || obj != null && obj instanceof Version
                && compareTo((Version) obj) == 0;
    }

    /**
     * Compares this version to the provided one, following the
     * <em>semantic versioning</em> specification. See
     * {@link #compare(Version, Version)} for more information. Here is a quote
     * from <a href="http://semver.org/">http://semver.org</a>:
     *
     * <p>
     * <em> Precedence refers to how versions are compared to each other when
     * ordered. Precedence MUST be calculated by separating the version into
     * major, minor, patch and pre-release identifiers in that order (Build
     * metadata does not figure into precedence). Precedence is determined by
     * the first difference when comparing each of these identifiers from left
     * to right as follows: Major, minor, and patch versions are always compared
     * numerically. Example: 1.0.0 &lt; 2.0.0 &lt; 2.1.0 &lt; 2.1.1. When major, minor,
     * and patch are equal, a pre-release version has lower precedence than a
     * normal version. Example: 1.0.0-alpha &lt; 1.0.0. Precedence for two
     * pre-release versions with the same major, minor, and patch version MUST
     * be determined by comparing each dot separated identifier from left to
     * right until a difference is found as follows: identifiers consisting of
     * only digits are compared numerically and identifiers with letters or
     * hyphens are compared lexically in ASCII sort order. Numeric identifiers
     * always have lower precedence than non-numeric identifiers. A larger set
     * of pre-release fields has a higher precedence than a smaller set, if all
     * of the preceding identifiers are equal. Example: 1.0.0-alpha &lt;
     * 1.0.0-alpha.1 &lt; 1.0.0-alpha.beta &lt; 1.0.0-beta &lt; 1.0.0-beta.2 &lt;
     * 1.0.0-beta.11 &lt; 1.0.0-rc.1 &lt; 1.0.0.
     * </em>
     * </p>
     *
     * @param other The version to compare to.
     * @return A value lower than 0 if this &lt; other, a value greater than 0
     *         if this &gt; other and 0 if this == other. The absolute value of
     *         the result has no semantical interpretation.
     */
    @Override
    public int compareTo(Version other) {
        return compare(this, other);
    }
}
