package Errors {

 interface _If1Arg
 {
    function fn(argA:Number);
 }

 class FuncDefErr67 implements _If1Arg
 {
    public function fn(argB:Number,argA:String):Number {};
 }
}
