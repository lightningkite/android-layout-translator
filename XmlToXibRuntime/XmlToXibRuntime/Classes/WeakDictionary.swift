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

public class WeakDictionary<Key: AnyObject, Value> {

    public init(){
    }
    private let lock = SpinLock()
    private var table: Dictionary<WeakObject<Key>, Value> = [:]
    public var count: Int {
        return table.count
    }
    public subscript(key: Key) -> Value? {
        get {
            checkClean()
            return lock.run {
                let key = WeakObject(key)
                cleanKey(key)
                return table[key]
            }
        }
        set(value) {
            lock.run {
                let key = WeakObject(key)
                if let value = value {
                    table[key] = value
                    updateKeyLocked(key)
                } else {
                    table.removeValue(forKey: key)
                }
            }
            checkClean()
        }
    }
    public func removeValue(forKey: Key) -> Value? {
        return table.removeValue(forKey: WeakObject(forKey))
    }
    public func getOrPut(_ from: Key, _ generate: ()->Value) -> Value {
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
    public func modify(_ from: Key, defaultValue:Value? = nil, modifier: (inout Value)->Void) {
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
    private func updateKeyLocked(_ key: WeakObject<Key>) {
        if let index = table.index(forKey: key) {
            table[index].key.object = key.object
        }
    }
    private func cleanKey(_ key: WeakObject<Key>) {
        if let index = table.index(forKey: key) {
            if !table[index].key.alive {
                table.remove(at: index)
            }
        }
    }
    public func clean(){
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

final class SpinLock {
    private var unfairLock = os_unfair_lock_s()
    func run<Result>(action: () -> Result) -> Result {
        os_unfair_lock_lock(&unfairLock)
        let result = action()
        os_unfair_lock_unlock(&unfairLock)
        return result
    }
    func runThrowing<Result>(action: () throws -> Result) throws -> Result {
        os_unfair_lock_lock(&unfairLock)
        do {
            let result = try action()
            os_unfair_lock_unlock(&unfairLock)
            return result
        } catch {
            os_unfair_lock_unlock(&unfairLock)
            throw error
        }
    }
}
