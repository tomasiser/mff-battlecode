University project for the [Battlecode](https://www.battlecode.org/) competition.

**How to install:**

1. Download Battlecode files: https://github.com/battlecode/battlecode-scaffold-2017/archive/master.zip
1. Run `gradlew` and `gradlew build`
1. Clone this repository to `src/KSTTForTheWin`

## Broadcasting

```java
import KSTTForTheWin.Broadcasting.Broadcaster;
// ...
static Broadcaster broadcaster;
// ...
    static void run...(RobotController rcon) throws GameActionException {
        // ...
        broadcaster = new Broadcaster(rcon);
```

How do I update my broadcaster?

```java
broadcaster.refresh(); // do this at the beginning of each round! it's a cheap command!
```

How to report that I found an enemy Archon?

```java
broadcaster.reportEnemyArchon(archonRobotId, location);
```

How to report that I need help?

```java
broadcaster.reportHelpNeeded(); // do not report each round!
```

How to find where the nearest situation is?

```java
MapLocation whereToGo = broadcaster.findNearestAction();
```

How do I report that the enemy Archon has died?

```java
broadcaster.reportEnemyArchonDied(archonRobotId);
```