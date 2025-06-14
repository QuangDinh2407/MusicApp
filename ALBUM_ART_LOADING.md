# Album Art Loading trong Notification

## 🎯 Tính Năng

### 📸 **Load Hình Album từ URL**
- ✅ **Glide Integration**: Sử dụng Glide để load hình từ `song.getCoverUrl()`
- ✅ **Smart Caching**: Cache hình để tránh reload không cần thiết
- ✅ **Fallback**: Hiển thị default cover khi load fail
- ✅ **Performance**: Tối ưu size 100x100px cho notification

### 🔄 **Workflow**

1. **Default First**: Hiển thị `default_cover` ngay lập tức
2. **Background Load**: Glide load hình từ URL trong background
3. **Update Notification**: Cập nhật notification khi load xong
4. **Cache Management**: Cache hình cho lần sử dụng tiếp

## 🔧 **Cấu Trúc Code**

### Method Chính
```java
private void loadAlbumArt(RemoteViews layout, Song song)
```

### Glide Configuration
```java
Glide.with(this)
    .asBitmap()
    .load(song.getCoverUrl())
    .placeholder(R.drawable.default_cover)
    .error(R.drawable.default_cover)
    .centerCrop()
    .override(100, 100)                    // Exact size
    .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache
    .into(CustomTarget<Bitmap>)
```

### Cache Management
```java
private Bitmap currentAlbumArt = null;

// Clear cache when changing songs
currentAlbumArt = null;
```

## ✨ **Ưu Điểm**

1. **Fast Loading**: Default image hiển thị ngay, real image load background
2. **Memory Efficient**: Cache bitmap để tránh reload
3. **Error Handling**: Graceful fallback to default image
4. **Performance**: 
   - Disk cache cho network requests
   - Override size để tối ưu memory
   - CenterCrop để fit notification layout

## 📱 **User Experience**

### Loading States
1. **Instant**: Default cover hiển thị ngay lập tức
2. **Background**: Notification vẫn hoạt động bình thường
3. **Update**: Hình thật hiển thị sau vài giây
4. **Cached**: Lần tiếp theo load instant từ cache

### Error Handling
- **Network Error**: Giữ default image
- **Invalid URL**: Fallback to default
- **Image Error**: Log error, show default

## 🎵 **Integration**

### Trong Expanded Layout
```xml
<ImageView
    android:id="@+id/notification_album_art"
    android:layout_width="100dp"
    android:layout_height="100dp"
    android:scaleType="centerCrop" />
```

### Automatic Updates
- Load khi tạo notification mới
- Clear cache khi đổi bài
- Re-use cache cho cùng một bài

## 🚀 **Performance Tips**

1. **Size Optimization**: 100x100px perfect cho notification
2. **Cache Strategy**: ALL để cache cả original và resized
3. **Memory Management**: Clear cache khi đổi bài
4. **Background Loading**: Không block UI thread 