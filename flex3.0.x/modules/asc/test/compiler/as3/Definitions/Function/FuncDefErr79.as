package Errors {
 
 interface _If2Arg
 {
    function fn(argA:Number,argB:String);
 }

 class FuncDefErr79 implements _If2Arg
 {
    public function fn(argA:Number):Number {};
 }
}
