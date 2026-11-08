# Jakarta / Angus Mail
-keep class jakarta.mail.** { *; }
-keep class com.sun.mail.** { *; }
-keep class org.eclipse.angus.** { *; }
-keep class jakarta.activation.** { *; }
-dontwarn jakarta.activation.**
-dontwarn com.sun.mail.**
-dontwarn jakarta.mail.**
-dontwarn java.awt.**
-dontwarn javax.security.**
