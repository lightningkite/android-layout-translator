OVERALL PLAN

- [ ] Generate outlets inside of XIB
- [ ] Invent some kind of access scheme for related views
- [ ] Test on basic file for layouts
- [ ] Many attributes wow

- Current Version Improvements
    - iOS
        - Finish XML -> XIB translations - 2 days
            - Layout only
            - Resource Translation
            - Generate XML class files
            - Store related views via extension properties
        - New Actuals - 2 days
        - Separate out RxProperty binding
        - Use structs over data classes?
    - Web
        - Potentially redo HTML translations, not totally necessary though
        - Use RxProperty binding
            - Create actuals
        - Use interfaces over data classes?
- Next Version
    - iOS
        - Activity -> ViewController
        - Fragment -> ViewController
        - Rx view binding actuals
    - Web
        - Activity -> ViewController
        - Fragment -> ViewController
        - Rx view binding actuals


Fine-Grained

- iOS
    - XML
        - Elements/Attributes: 70 * 15m = 17.5 hours
        - Layout Class Generation: 4 hours
        - Resource Translation: 5 hours
    - Swift
        - SystemTime.swift - 0.1 hours
        - UUID.ext.swift - 0.1 hours
        - newEmptyView.swift - 0.1 hours
        - TextStyling.swift - 4 hours
        - MaterialSegmentedControl.swift - 0.5 hour
    - RxBinding
        - ToggleButton.swift - 0.5 hour
        - TimeButton.swift - 0.5 hour
        - Dropdown.swift - 0.5 hour
        - DateButton.swift - 0.5 hour
        - CircleImageView.swift - 0.5 hour
        - LinearLayout.binding.swift - 0.5 hour
        - Dropdown.binding.swift - 0.5 hour
        - SwapView.binding.swift - 0.5 hour
        - CompoundButton.binding.swift - 0.5 hour
        - ToggleButton.binding.swift - 0.5 hour
        - DateButton.binding.swift - 0.5 hour
        - ViewFlipper.binding.swift - 0.5 hour
    - Tie together and use - 8 hours
- Web
    - Use RxProperty binding - 8 hours
    - Tie together and use - 8 hours
    - Use CSS Styles? - 16 hours


BEWARE

- font
- lineSpacingMultiplier
- letterSpacing
- textAllCaps
- bold/italic