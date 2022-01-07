import './resources/resources.scss'
import {TestMarginBinding} from "./resources/layouts/TestMarginBinding";

console.log("Inflating TestToggleBinding")
const x = TestMarginBinding.inflate()
console.log("Appending view ", x._root)
document.body.appendChild(x._root)
console.log("Done")
