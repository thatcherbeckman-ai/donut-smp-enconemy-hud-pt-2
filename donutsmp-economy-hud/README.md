# DonutSMP Economy HUD 0.2.0 (source)

Fabric 1.21.11 / Java 21. Open settings with **O**. Displays a balance and a profit/loss line graph, tracking changes between recognized balance messages.

**Limitations:** The server message format is not fully verified. This build watches Fabric GAME messages, not all chat messages. It recognizes standalone `$117,550,323,393` lines or `Balance: $...` lines. It does not automatically issue `/bal`, and it cannot infer unseen transactions. Session history is in memory only; restarting Minecraft resets it. The graph is centered at zero, scaled to the largest absolute session change. Manual balance input is supported.

Build via GitHub Actions with Java 21 and Gradle 9.2.1, or your own compatible Fabric Loom setup. This source has **not been compiled or tested in Minecraft**.
