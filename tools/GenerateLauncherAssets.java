import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Packages the ImageGen foreground into Android launcher resources without redesigning it.
 * Run from the repository root: java tools/GenerateLauncherAssets.java
 */
public class GenerateLauncherAssets {
    private static final Path RES = Path.of("GPSTest/src/main/res");
    private static final Color TOP = new Color(0x293F8B);
    private static final Color BOTTOM = new Color(0x111C46);
    private static BufferedImage foreground;

    public static void main(String[] args) throws IOException {
        BufferedImage source = ImageIO.read(Path.of("icons/w-gpstest-launcher-foreground.png").toFile());
        int minX=source.getWidth(), minY=source.getHeight(), maxX=0, maxY=0;
        for (int y=0; y<source.getHeight(); y++) for (int x=0; x<source.getWidth(); x++) {
            if ((source.getRGB(x,y) >>> 24) >= 16) {
                minX=Math.min(minX,x); minY=Math.min(minY,y);
                maxX=Math.max(maxX,x); maxY=Math.max(maxY,y);
            }
        }
        double cx=(minX+maxX)/2.0, cy=(minY+maxY)/2.0, radius=0;
        for (int y=minY; y<=maxY; y++) for (int x=minX; x<=maxX; x++) {
            if ((source.getRGB(x,y) >>> 24) >= 16) radius=Math.max(radius,Math.hypot(x-cx,y-cy));
        }
        if (radius <= 0) throw new IllegalArgumentException("Empty launcher artwork");
        foreground = new BufferedImage(432,432,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = graphics(foreground);
        // Keep the complete emblem within the central 66 dp safe circle of a 108 dp layer.
        double scale=31.5*4/radius;
        AffineTransform transform = AffineTransform.getTranslateInstance(216-cx*scale,216-cy*scale);
        transform.scale(scale,scale);
        g.drawImage(source,transform,null); g.dispose();
        save(foreground,RES.resolve("drawable-xxxhdpi/w_launcher_art.png"));
        BufferedImage mono = new BufferedImage(432,432,BufferedImage.TYPE_INT_ARGB);
        for (int y=0; y<432; y++) for (int x=0; x<432; x++) {
            mono.setRGB(x,y,(foreground.getRGB(x,y)&0xff000000)|0xffffff);
        }
        save(mono,RES.resolve("drawable-xxxhdpi/w_launcher_mono_art.png"));
        String[] densities={"mdpi","hdpi","xhdpi","xxhdpi","xxxhdpi"};
        int[] sizes={48,72,96,144,192};
        for (int i=0;i<densities.length;i++) {
            save(tile(sizes[i],false),RES.resolve("mipmap-"+densities[i]+"/w_launcher_legacy.png"));
            save(tile(sizes[i],true),RES.resolve("mipmap-"+densities[i]+"/w_launcher_legacy_round.png"));
        }
        save(tile(512,false),Path.of("icons/w-gpstest-icon-preview.png"));
        System.out.println("Created adaptive color/monochrome layers, 10 legacy assets, and a preview image.");
    }
    private static Graphics2D graphics(BufferedImage image) {
        Graphics2D g=image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
        return g;
    }
    private static BufferedImage tile(int size,boolean round) {
        BufferedImage image=new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=graphics(image);
        Shape mask=round?new Ellipse2D.Double(0,0,size,size):
            new RoundRectangle2D.Double(0,0,size,size,size*0.28,size*0.28);
        g.setPaint(new GradientPaint(0,0,TOP,0,size,BOTTOM)); g.fill(mask);
        g.setClip(mask);
        double scale=size*1.5/432;
        AffineTransform transform=AffineTransform.getTranslateInstance(-size*0.25,-size*0.25);
        transform.scale(scale,scale); g.drawImage(foreground,transform,null); g.dispose();
        return image;
    }
    private static void save(BufferedImage image,Path file) throws IOException {
        Files.createDirectories(file.getParent()); ImageIO.write(image,"png",file.toFile());
    }
}
