//
//  File.swift
//  XmlToXibRuntime
//
//  Created by Joseph Ivie on 11/17/21.
//

import M13Checkbox


open class M13RadioButton: M13Checkbox {
    open override func toggleCheckState(_ animated: Bool = false) {
        switch checkState {
        case .checked:
            break
        case .unchecked:
            setCheckState(.checked, animated: animated)
            break
        case .mixed:
            setCheckState(.checked, animated: animated)
            break
        }
    }
}
