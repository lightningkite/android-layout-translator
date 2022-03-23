//
//  ContainerView.swift
//  Cosmos
//
//  Created by Joseph Ivie on 11/17/21.
//

import UIKit

open class ContainerView: UIView {
    open override func hitTest(_ point: CGPoint, with event: UIEvent?) -> UIView? {
        if self.isHidden || self.alpha < 0.01 { return nil }
        guard self.bounds.contains(point) else { return nil }
        if let first = subviews.first {
            let adjusted = CGPoint(
                x: (point.x - first.frame.minX).clamped(to: 0...first.bounds.size.width),
                y: (point.y - first.frame.minY).clamped(to: 0...first.bounds.size.height)
            )
            return first.hitTest(adjusted, with: event)
        }
        return nil
    }
    private var _containedView: UIView? {
        guard let first = subviews.first else { return nil }
        if let first = first as? ContainerView {
            return first._containedView
        }
        return first
    }
    private var childObserver: NSKeyValueObservation?
    private var childObserver2: NSKeyValueObservation?
    public var finalView: UIView? {
        if let sub = self.subviews.first {
            if let subC = sub as? ContainerView {
                return subC.finalView
            } else {
                return sub
            }
        } else {
            return nil
        }
    }
    open override func didAddSubview(_ subview: UIView) {
        super.didAddSubview(subview)
        if subviews.count == 1 {
            childObserver = subview.observe(\UIView.isHidden, changeHandler: { [weak self] (view, change) in
                self?.isHidden = view.isHidden
            })
            self.isHidden = subview.isHidden
            
            childObserver2 = finalView?.observe(\UIView.alpha, changeHandler: { [weak self] (view, change) in
                guard let self = self else { return }
                self.backgroundLayerForThis?.opacity = Float(view.alpha)
            })
            self.backgroundLayerForThis?.opacity = Float(finalView?.alpha ?? 1)
        }
    }
}
extension Comparable {
    func clamped(to limits: ClosedRange<Self>) -> Self {
        return min(max(self, limits.lowerBound), limits.upperBound)
    }
}

func generateSequence<T>(seed: T?, next: @escaping (T)->T?) -> GenerateSequence<T> {
    return GenerateSequence(seed: seed, nextLambda: next)
}

struct GenerateSequence<Element>: Sequence, IteratorProtocol {
    var seed: Element?
    let nextLambda: (Element)->Element?

    mutating func next() -> Element? {
        defer {
            if let input = seed {
                seed = nextLambda(input)
            }
        }

        return seed
    }
}
