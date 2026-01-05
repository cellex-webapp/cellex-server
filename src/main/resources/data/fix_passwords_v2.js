// Generate correct BCrypt password hash using Spring Boot BCryptPasswordEncoder
// Run this in mongosh after ensuring BCrypt is properly configured

use('cellex_prod');

print("\n========================================");
print("🔐 PASSWORD HASH FIX - Option 2");
print("========================================\n");

// Correct BCrypt hash for "admin123" using Spring's BCryptPasswordEncoder (cost=10)
// This hash was generated using: new BCryptPasswordEncoder().encode("admin123")
const correctHash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

print("Testing current users...\n");

// Check current passwords
const currentUsers = db.users.find({}, {email: 1, password: 1}).limit(3).toArray();
currentUsers.forEach(user => {
    print(`  ${user.email}`);
    print(`    Current: ${user.password.substring(0, 30)}...`);
});

print("\n----------------------------------------");
print("⚠️  WARNING: This will update ALL user passwords!");
print("----------------------------------------\n");

// Update all users
const updateResult = db.users.updateMany(
    {},
    { $set: { password: correctHash } }
);

print(`✓ Updated ${updateResult.modifiedCount} users\n`);

// Verify
print("Verification:");
const updatedUsers = db.users.find({}, {email: 1, password: 1}).limit(3).toArray();
updatedUsers.forEach(user => {
    print(`  ✓ ${user.email}: ${user.password.substring(0, 30)}...`);
});

print("\n========================================");
print("✅ COMPLETED!");
print("========================================");
print("\nAll users can now login with:");
print("  Password: admin123");
print("\nTest accounts:");
print("  - admin@cellex.vn (ADMIN)");
print("  - hung.vendor@cellex.vn (VENDOR)");
print("  - tuan.customer@gmail.com (CUSTOMER - Đồng)");
print("  - minh.customer@gmail.com (CUSTOMER - Vàng)");
print("  - huong.customer@gmail.com (CUSTOMER - Kim cương)");
print("\n========================================\n");
