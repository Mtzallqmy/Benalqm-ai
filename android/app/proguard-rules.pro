# Keep the manifest-driven TWA entry point and our launcher activities readable to R8.
-keep class ai.moataz.app.MainActivity { *; }
-keep class ai.moataz.app.MoatazTwaActivity { *; }
-keep class ai.moataz.app.MoatazWebViewActivity { *; }
