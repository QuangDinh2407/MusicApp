const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

// Hàm tự động cập nhật title_lower khi có bài hát mới hoặc cập nhật
exports.updateSongTitleLower = functions.firestore
    .document('songs/{songId}')
    .onWrite((change, context) => {
        const songData = change.after.exists ? change.after.data() : null;

        // Nếu document bị xóa, không cần làm gì
        if (!songData) {
            return null;
        }

        const title = songData.title;
        if (!title) {
            return null;
        }

        // Cập nhật trường title_lower
        return change.after.ref.update({
            title_lower: title.toLowerCase()
        });
    });