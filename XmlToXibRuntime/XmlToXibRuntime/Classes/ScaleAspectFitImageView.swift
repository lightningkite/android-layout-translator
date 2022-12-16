import UIKit

// known-good: Xcode 8.2.1

/**
 
 UIImageView subclass which works with Auto Layout to try
 to maintain the same aspect ratio as the image it displays.
 
 This is unlike the usual behavior of UIImageView, where the
 scaleAspectFit content mode only affects what the view displays
 and not the size it prefers, and so it does not play
 well with AL constraints. In particular, UIImageView.intrinsicContentSize
 always returns each of the intrinsic size dimensions of the image
 itself, not a size that adjusts to reflect constraints on the
 view. So if you constrain the width of a UIImageView, for example,
 the view's intrinsic content size still declares a preferred
 height based on the image's intrinsic height, rather than the
 displayed height produced by the scaleAspectFit content mode.
 
 In contrast, this subclass has a few notable properties:
 
 - If you externally constraint one dimension, its internal constraints
 will then adjust the other dimension so it holds the image's aspect
 ratio.
 
 - Uses a low layout priority to do this. So if you externally
 require it to have an incorrect aspect ratio, you do not get conflicts.
 
 - Still uses the scaleAspectFit content mode internally, so if a
 client requires an incorrect aspect, you still get scaleAspectFit
 behavior to determining what is displayed within whatever
 dimensionsare finally used.
 
 - It is a subclass of UIImageView and supports all of UIImageView's
 initializers, so it is a drop-in substitute.
 
 */
public class ScaleAspectFitImageView : UIImageView
{
    /// constraint to maintain same aspect ratio as the image
    private var aspectRatioConstraint:NSLayoutConstraint? = nil
    
    required public init?(coder aDecoder: NSCoder)
    {
        super.init(coder:aDecoder)
        self.setup()
    }
    
    public override init(frame:CGRect)
    {
        super.init(frame:frame)
        self.setup()
    }
    
    public override init(image: UIImage!)
    {
        super.init(image:image)
        self.setup()
    }
    
    public override init(image: UIImage!, highlightedImage: UIImage?)
    {
        super.init(image:image,highlightedImage:highlightedImage)
        self.setup()
    }
    
    override public var image: UIImage? {
        didSet {
            self.updateAspectRatioConstraint()
        }
    }
    
    private func setup()
    {
        self.contentMode = .scaleAspectFit
        self.updateAspectRatioConstraint()
    }
    
    /// Removes any pre-existing aspect ratio constraint, and adds a new one based on the current image
    private func updateAspectRatioConstraint()
    {
        // remove any existing aspect ratio constraint
        if let c = self.aspectRatioConstraint {
            self.removeConstraint(c)
        }
        self.aspectRatioConstraint = nil
        
        if let imageSize = image?.size, imageSize.height != 0
        {
            
            let aspectRatio = imageSize.width / imageSize.height
            let c = NSLayoutConstraint(item: self, attribute: .width,
                                       relatedBy: .equal,
                                       toItem: self, attribute: .height,
                                       multiplier: aspectRatio, constant: 0)
            // a priority above fitting size level and below low
            
            c.priority = UILayoutPriority(1000)
            self.addConstraint(c)
            self.aspectRatioConstraint = c
        }
    }
}
