# manaba4j
筑波大学 manabaのKotlin実装

## 使い方
### manabaにログイン
```kotlin
import dev.itsu.manaba.Manaba

Manaba.login("UTID-13 or UTID-NAME", "PASSWORD")
```

### [Assignment](https://github.com/itsu-dev/manaba4j/blob/master/src/main/kotlin/model/Assignment.kt)（未提出課題）と[Course](https://github.com/itsu-dev/manaba4j/blob/master/src/main/kotlin/model/Course.kt)（コース）の取得
```kotlin
import dev.itsu.manaba.Manaba
import dev.itsu.manaba.model.Assignment
import dev.itsu.manaba.model.Course

Manaba.login("UTID-13 or UTID-NAME", "PASSWORD")
val assignments: List<Assignment> = Manaba.getAssignments() // 未提出課題一覧
val courses: List<Course> = Manaba.getCourses() // コース一覧
```