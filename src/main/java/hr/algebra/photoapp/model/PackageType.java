package hr.algebra.photoapp.model;

public enum PackageType {
    FREE(2_000_000, 5),
    PRO(5_000_000, 20),
    GOLD(Long.MAX_VALUE, Integer.MAX_VALUE);

    private final long maxFileSize;
    private final int maxUploadsPerDay;

    PackageType(long maxFileSize, int maxUploadsPerDay) {
        this.maxFileSize = maxFileSize;
        this.maxUploadsPerDay = maxUploadsPerDay;
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public int getMaxUploadsPerDay() {
        return maxUploadsPerDay;
    }
}

