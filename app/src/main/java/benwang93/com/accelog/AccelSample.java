package benwang93.com.accelog;

/**
 * Struct to store a single acceleration data point.
 *
 * Created by Ben Wang on 9/10/2015.
 */
public class AccelSample {
    public long time;   // time in milliseconds
    public double aX;      // acceleration on X axis in Gs
    public double aY;      // acceleration on Y axis in Gs
    public double aZ;      // acceleration on Z axis in Gs
}
