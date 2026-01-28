# 📚 HISTORY DATABASE - HƯỚNG DẪN

## ✨ TÍNH NĂNG

Lưu lịch sử wallpaper đã set vào Room Database để:
- 📖 Xem lại wallpaper đã set
- 🔄 Set lại wallpaper cũ nhanh chóng
- 📊 Thống kê wallpaper yêu thích
- 🎯 Track wallpaper đang dùng

---

## 📁 FILES ĐÃ TẠO/CHỈNH SỬA

### **1. HistoryEntity.kt** (NEW)
- Path: `app/src/main/java/com/nomyek/myapplication/data/entity/HistoryEntity.kt`
- Entity lưu lịch sử wallpaper
- Fields:
  - `id` - ID tự động tăng
  - `originalPath` - Path GIF gốc
  - `title` - Tên wallpaper
  - `category` - Category
  - `cropX, cropY, cropWidth, cropHeight` - Crop info
  - `scaleFactor, translationX, translationY` - Transform info
  - `wallpaperType` - LIVE hoặc STATIC
  - `isCurrentlySet` - Đang set hay không
  - `createdAt` - Thời gian tạo
  - `lastUsedAt` - Lần dùng cuối

### **2. HistoryDao.kt** (NEW)
- Path: `app/src/main/java/com/nomyek/myapplication/data/dao/HistoryDao.kt`
- DAO cho History operations
- Methods:
  - `insert()` - Insert history
  - `getAllHistory()` - Get all history
  - `getCurrentWallpaper()` - Get wallpaper đang set
  - `markAsCurrentlySet()` - Mark wallpaper là đang dùng
  - `deleteOldHistory()` - Delete history cũ
  - `searchHistory()` - Search history

### **3. AppDatabase.kt** (UPDATED)
- Thêm `HistoryEntity` vào entities
- Bump version: 2 → 3
- Thêm `historyDao()`

### **4. Repository.kt** (UPDATED)
- Thêm `HistoryDao` vào constructor
- Thêm History methods:
  - `insertHistory()`
  - `getAllHistory()`
  - `getCurrentWallpaper()`
  - `markHistoryAsCurrentlySet()`
  - `deleteHistory()`
  - `clearAllHistory()`
  - `searchHistory()`

### **5. EditGitViewModel.kt** (UPDATED)
- Update `setLiveWallpaper()`:
  1. Tạo `HistoryEntity` từ wallpaper + cropInfo
  2. Insert vào database
  3. Set wallpaper
  4. Mark as currently set
  
- Update `setStaticWallpaper()`:
  - Tương tự nhưng với type = STATIC

---

## 🗄️ DATABASE SCHEMA

```sql
CREATE TABLE history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    originalPath TEXT NOT NULL,
    title TEXT NOT NULL,
    category TEXT NOT NULL,
    cropX INTEGER NOT NULL,
    cropY INTEGER NOT NULL,
    cropWidth INTEGER NOT NULL,
    cropHeight INTEGER NOT NULL,
    scaleFactor REAL NOT NULL DEFAULT 1.0,
    translationX REAL NOT NULL DEFAULT 0.0,
    translationY REAL NOT NULL DEFAULT 0.0,
    wallpaperType TEXT NOT NULL,
    isCurrentlySet INTEGER NOT NULL DEFAULT 0,
    createdAt INTEGER NOT NULL,
    lastUsedAt INTEGER NOT NULL
);
```

---

## 🚀 USAGE

### **Lưu Wallpaper vào History:**

```kotlin
// Trong EditGitViewModel
fun setLiveWallpaper(cropInfo: GifCropInfo) {
    viewModelScope.launch {
        // 1. Tạo HistoryEntity
        val historyEntity = HistoryEntity.fromWallpaperAndCropInfo(
            wallpaper = currentWallpaper,
            cropInfo = cropInfo,
            wallpaperType = HistoryEntity.WallpaperType.LIVE
        )
        
        // 2. Insert vào database
        val historyId = repository.insertHistory(historyEntity)
        
        // 3. Set wallpaper
        GifUtils.setLiveGifWallpaper(context, cropInfo)
        
        // 4. Mark as currently set
        repository.markHistoryAsCurrentlySet(historyId)
    }
}
```

### **Get History:**

```kotlin
// Get all history
repository.getAllHistory()
    .collect { historyList ->
        // Display history
    }

// Get currently set wallpaper
val currentWallpaper = repository.getCurrentWallpaper()

// Search history
repository.searchHistory("nature")
    .collect { searchResults ->
        // Display results
    }
```

### **Reapply Wallpaper từ History:**

```kotlin
fun reapplyWallpaper(history: HistoryEntity) {
    val cropInfo = history.toCropInfo()
    
    when (history.wallpaperType) {
        HistoryEntity.WallpaperType.LIVE -> {
            GifUtils.setLiveGifWallpaper(context, cropInfo)
        }
        HistoryEntity.WallpaperType.STATIC -> {
            GifUtils.setStaticGifWallpaper(context, cropInfo)
        }
    }
    
    // Update last used time
    repository.updateHistoryLastUsedTime(history.id)
    repository.markHistoryAsCurrentlySet(history.id)
}
```

---

## 📊 DATA FLOW

```
User sets wallpaper
  ↓
EditGitViewModel.setLiveWallpaper()
  ↓
1. Create HistoryEntity
   - originalPath: "assets/gifs/smile.gif"
   - title: "Silly Smile"
   - category: "Smile"
   - cropInfo: {x, y, width, height, scale, translation}
   - wallpaperType: LIVE
  ↓
2. repository.insertHistory(historyEntity)
   - Returns historyId
  ↓
3. GifUtils.setLiveGifWallpaper()
   - Set wallpaper
  ↓
4. repository.markHistoryAsCurrentlySet(historyId)
   - Mark as currently set
   - Unmark others
  ↓
Done! History saved ✅
```

---

## 🎯 USE CASES

### **1. History Screen (TO BE IMPLEMENTED)**
```kotlin
@Composable
fun HistoryScreen(viewModel: HistoryViewModel) {
    val history by viewModel.history.collectAsState()
    
    LazyColumn {
        items(history) { item ->
            HistoryItem(
                history = item,
                onReapply = { viewModel.reapplyWallpaper(it) },
                onDelete = { viewModel.deleteHistory(it) }
            )
        }
    }
}
```

### **2. Currently Set Wallpaper Widget**
```kotlin
@Composable
fun CurrentWallpaperWidget() {
    val current = viewModel.getCurrentWallpaper()
    
    current?.let {
        Card {
            Text("Currently set: ${it.title}")
            Text("Type: ${it.wallpaperType}")
            Button("Reapply") {
                viewModel.reapplyWallpaper(it)
            }
        }
    }
}
```

### **3. Statistics**
```kotlin
suspend fun getStatistics() {
    val totalCount = repository.getHistoryCount()
    val liveCount = repository.getHistoryByType(LIVE).first().size
    val staticCount = repository.getHistoryByType(STATIC).first().size
    
    Log.d("Stats", "Total: $totalCount, Live: $liveCount, Static: $staticCount")
}
```

---

## 🔧 MAINTENANCE

### **Auto cleanup old history:**
```kotlin
// In background worker or app startup
viewModelScope.launch {
    // Keep only recent 50 items
    repository.deleteOldHistory(keepCount = 50)
}
```

### **Clear all history:**
```kotlin
repository.clearAllHistory()
```

---

## ✅ DATABASE MIGRATION

**Version 2 → 3:**
- Added `history` table
- No breaking changes to existing tables

Room will auto-create new table on first launch.

---

## 🎉 BENEFITS

✅ **Track wallpaper usage** - Know what wallpapers you set  
✅ **Quick reapply** - Set lại wallpaper cũ instant  
✅ **Statistics** - Analyze usage patterns  
✅ **Backup** - Lưu crop settings  
✅ **Search** - Find old wallpapers  

---

## 📝 TODO (Future)

- [ ] Create History Screen UI
- [ ] Add export/import history feature
- [ ] Add sync with cloud
- [ ] Add favorites in history
- [ ] Add sharing history

---

**History is now saved to database! 📚✨**

