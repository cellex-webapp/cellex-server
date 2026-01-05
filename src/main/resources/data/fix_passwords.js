// ============================================
// FIX PASSWORD - Update BCrypt hashes
// ============================================
// Chạy script này để update password cho tất cả users
// Password: admin123

use('cellex_prod');

// BCrypt hash cho "admin123" với cost factor 10
// Generated từ: https://bcrypt-generator.com hoặc backend
const correctPasswordHash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

print("\n🔐 Updating passwords for all users...\n");

// Update tất cả users với password mới
const result = db.users.updateMany(
    {},
    { $set: { password: correctPasswordHash } }
);

print(`✓ Updated ${result.modifiedCount} users with new password hash\n`);

// Verify update
print("Verifying updates:");
const sampleUsers = db.users.find({}, {email: 1, password: 1}).limit(3).toArray();
sampleUsers.forEach(user => {
    print(`  - ${user.email}: ${user.password.substring(0, 20)}...`);
});

print("\n✅ Password update completed!");
print("All users can now login with password: admin123\n");
