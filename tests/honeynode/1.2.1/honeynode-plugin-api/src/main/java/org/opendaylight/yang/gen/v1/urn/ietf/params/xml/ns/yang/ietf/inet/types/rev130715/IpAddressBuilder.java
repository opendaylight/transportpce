
package org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * * The purpose of generated class in src/main/java for Union types is to
 * create * new instances of unions from a string representation. In some cases
 * it is * very difficult to automate it since there can be unions such as
 * (uint32 - * uint16), or (string - uint32). * * The reason behind putting it
 * under src/main/java is: This class is generated * in form of a stub and needs
 * to be finished by the user. This class is * generated only once to prevent
 * loss of user code. *
 */
public class IpAddressBuilder {
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "(([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(%[\\p{N}\\p{L}]+)?");
    private static final Pattern IPV6_PATTERN1 = Pattern.compile(
            "((:|[0-9a-fA-F]{0,4}):)([0-9a-fA-F]{0,4}:){0,5}((([0-9a-fA-F]{0,4}:)?(:|[0-9a-fA-F]{0,4}))|(((25[0-5]|2[0-4][0-9]|[01]?[0-9]?[0-9])\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9]?[0-9])))(%[\\p{N}\\p{L}]+)?");
    private static final Pattern IPV6_PATTERN2 = Pattern
            .compile("(([^:]+:){6}(([^:]+:[^:]+)|(.*\\..*)))|((([^:]+:)*[^:]+)?::(([^:]+:)*[^:]+)?)(%.+)?");

    private IpAddressBuilder() {
    }

    /*
     * * public static IpAddress getDefaultInstance(java.lang.String defaultValue) {
     * * throw new java.lang.UnsupportedOperationException("Not yet implemented"); }
     */ public static IpAddress getDefaultInstance(String defaultValue) {
        final Matcher ipv4Matcher = IPV4_PATTERN.matcher(defaultValue);
        if (ipv4Matcher.matches()) {
            if (IPV6_PATTERN1.matcher(defaultValue).matches() && IPV6_PATTERN2.matcher(defaultValue).matches()) {
                throw new IllegalArgumentException(
                        String.format("Cannot create IpAddress from \"%s\", matches both %s and %s", defaultValue,
                                Ipv4Address.class.getSimpleName(), Ipv6Address.class.getSimpleName()));
            }
            return new IpAddress(new Ipv4Address(defaultValue));
        } else if (IPV6_PATTERN1.matcher(defaultValue).matches() && IPV6_PATTERN2.matcher(defaultValue).matches()) {
            return new IpAddress(new Ipv6Address(defaultValue));
        } else {
            throw new IllegalArgumentException("Cannot create IpAddress from " + defaultValue);
        }
    }
}
