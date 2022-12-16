//
//  extensionProperty.swift
//  ButterflyTemplate
//
//  Created by Joseph Ivie on 8/21/19.
//  Copyright © 2019 Joseph Ivie. All rights reserved.
//

import Foundation

private class WeakObject<T: AnyObject>: Equatable, Hashable, CustomStringConvertible {
    var identifierNumber: Int
    weak var object: T?
    init(_ object: T) {
        self.object = object
        self.identifierNumber = Int(bitPattern: ObjectIdentifier(object)) >> 2
    }
    init(noRef object: T) {
        self.identifierNumber = Int(bitPattern: ObjectIdentifier(object)) >> 2
    }
    init(identifier: ObjectIdentifier) {
        self.identifierNumber = Int(bitPattern: identifier) >> 2
    }

    func hash(into hasher: inout Hasher) {
        hasher.combine(identifierNumber)
    }

    var alive: Bool { return object !== nil }

    static func == (lhs: WeakObject<T>, rhs: WeakObject<T>) -> Bool {
        return lhs.identifierNumber == rhs.identifierNumber
    }
    
    var description: String {
        return String(describing: identifierNumber)
    }
}

internal class ExtensionProperty<On: AnyObject, T> {

    static internal func selfTest(){
        let exampleItem = NSObject()
        let ext = ExtensionProperty<NSObject, Int>()
        
        DispatchQueue.global(qos: .background).async {
            var delayedChecks: Array<()->Void> = []
            let countToTest = 100000
            for i in 0...countToTest {
                let newItem = NSObject()
                assert(ext.get(newItem) == nil)
                ext.set(newItem, i)
                delayedChecks.append {
                    let newVal = ext.get(newItem)
                    assert(newVal == i)
                }
                if i % 100 == 0 {
//                     print("Running checks...")
                    for check in delayedChecks {
                        check()
                    }
                    delayedChecks = []
                }
            }
//             print("Count is \(ext.count).")
        }

        ext.set(exampleItem, 1)
        assert(ext.get(exampleItem) == 1)
        ({ () in
            let exampleItem2 = NSObject()
            assert(ext.get(exampleItem2) == nil)
        })()
    }

    internal init(){
    }
    private let lock = SpinLock()
    private var table: Dictionary<WeakObject<On>, T> = [:]
    internal var count: Int {
        return table.count
    }

    internal func get(_ from: ObjectIdentifier) -> T? {
        checkClean()
        return lock.run {
            let key = WeakObject<On>(identifier: from)
            cleanKey(key)
            return table[key]
        }
    }
    internal func remove(_ from: On) {
        checkClean()
        lock.run {
            let key = WeakObject(noRef: from)
            cleanKey(key)
            table.removeValue(forKey: WeakObject(noRef: from))
        }
    }
    internal func get(_ from: On) -> T? {
        checkClean()
        return lock.run {
            let key = WeakObject(from)
            cleanKey(key)
            return table[key]
        }
    }
    internal func getOrPut(_ from: On, _ generate: ()->T) -> T {
        checkClean()
        return lock.run {
            let key = WeakObject(from)
            cleanKey(key)
            if let value = table[key] { return value }
            let generated = generate()
            table[key] = generated
            return generated
        }
    }
    internal func modify(_ from: On, defaultValue:T? = nil, modifier: (inout T)->Void) {
        lock.run {
            let key = WeakObject(from)
            cleanKey(key)
            if var current = table[key] {
                modifier(&current)
                table[key] = current
            } else if var defaultValue = defaultValue {
                modifier(&defaultValue)
                table[key] = defaultValue
                updateKeyLocked(key)
            }
        }
        checkClean()
    }
    internal func set(_ from: On, _ value: T?) {
        lock.run {
            let key = WeakObject(from)
            if let value = value {
                table[key] = value
                updateKeyLocked(key)
            } else {
                table.removeValue(forKey: key)
            }
        }
        checkClean()
    }
    private func updateKeyLocked(_ key: WeakObject<On>) {
        if let index = table.index(forKey: key) {
            table[index].key.object = key.object
        }
    }
    private func cleanKey(_ key: WeakObject<On>) {
        if let index = table.index(forKey: key) {
            if !table[index].key.alive {
                table.remove(at: index)
            }
        }
    }
    internal func clean(){
        lock.run {
            let keysToPurge = table.keys.filter { !$0.alive }
            for key in keysToPurge {
                table.removeValue(forKey: key)
            }
        }
    }

    var lastClean = CFAbsoluteTimeGetCurrent()
    var cleanInterval: CFTimeInterval = 10
    private func checkClean(){
        let now = CFAbsoluteTimeGetCurrent()
        if now - lastClean > cleanInterval {
            lastClean = now
            clean()
        }
    }
}

