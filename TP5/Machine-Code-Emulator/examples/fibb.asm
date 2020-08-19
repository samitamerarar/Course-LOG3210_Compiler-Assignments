// unsigned int fib(unsigned int n){
//    unsigned int i = n - 1, a = 1, b = 0, c = 0, d = 1, t;
//    if (n <= 0)
//      return 0;
//    while (i > 0){
//      if (i % 2 == 1){
//        t = d*(b + a) + c*b;
//        a = d*b + c*a;
//        b = t;
//      }
//      t = d*(2*c + d);
//      c = c*c + d*d;
//      d = t;
//      i = i / 2;
//    }
//    return a + b;
//  }

PRINT "Please enter the number of the fibonacci suite to compute:"
INPUT n

//    if (n <= 0)
//      return 0;
LD R0, n
BGTZ R0, validInput
PRINT #0
BR end

validInput:
//    unsigned int i = n - 1, a = 1, b = 0, c = 0, d = 1, t;
DEC R0
ST i, R0
ST a, #1
ST b, #0
ST c, #0
ST d, #1

//    while (i > 0){
beginWhile:
LD R0, i
BLETZ R0, printResult

//      if (i % 2 == 1){
MOD R0, R0, #2
DEC R0
BNETZ R0, afterIf

CLEAR

//        t = d*(b + a) + c*b;
//        a = d*b + c*a;
//        b = t;

// TODO:: PUT THE BLOCK 1 HERE !
LD R0, b
// Life_IN  : [@i]
// Life_OUT : [@b, @i]
// Next_IN  : 
// Next_OUT : @b:[2]

LD R1, a
// Life_IN  : [@b, @i]
// Life_OUT : [@a, @b, @i]
// Next_IN  : @b:[2]
// Next_OUT : @a:[2], @b:[2]

ADD R0, R0, R1
// Life_IN  : [@a, @b, @i]
// Life_OUT : [@i, @t0]
// Next_IN  : @a:[2], @b:[2]
// Next_OUT : @t0:[4]

LD R1, d
// Life_IN  : [@i, @t0]
// Life_OUT : [@d, @i, @t0]
// Next_IN  : @t0:[4]
// Next_OUT : @d:[4], @t0:[4]

MUL R0, R1, R0
// Life_IN  : [@d, @i, @t0]
// Life_OUT : [@d, @i, @t1]
// Next_IN  : @d:[4], @t0:[4]
// Next_OUT : @t1:[5]

ST t1, R0
// Life_IN  : [@d, @i, @t1]
// Life_OUT : [@d, @i]
// Next_IN  : @t1:[5]
// Next_OUT : 

LD R0, c
// Life_IN  : [@d, @i]
// Life_OUT : [@c, @d, @i]
// Next_IN  : 
// Next_OUT : @c:[8]

LD R1, b
// Life_IN  : [@c, @d, @i]
// Life_OUT : [@b!, @c, @d, @i]
// Next_IN  : @c:[8]
// Next_OUT : @b!:[8], @c:[8]

MUL R0, R0, R1
// Life_IN  : [@b!, @c, @d, @i]
// Life_OUT : [@c, @d, @i, @t2]
// Next_IN  : @b!:[8], @c:[8]
// Next_OUT : @t2:[10]

LD R1, t1
// Life_IN  : [@c, @d, @i, @t2]
// Life_OUT : [@c, @d, @i, @t1!, @t2]
// Next_IN  : @t2:[10]
// Next_OUT : @t1!:[10], @t2:[10]

ADD R0, R1, R0
// Life_IN  : [@c, @d, @i, @t1!, @t2]
// Life_OUT : [@c, @d, @i, @t3]
// Next_IN  : @t1!:[10], @t2:[10]
// Next_OUT : @t3:[11]

ADD R0, #0, R0
// Life_IN  : [@c, @d, @i, @t3]
// Life_OUT : [@c, @d, @i, @t]
// Next_IN  : @t3:[11]
// Next_OUT : @t:[12]

ST t, R0
// Life_IN  : [@c, @d, @i, @t]
// Life_OUT : [@c, @d, @i]
// Next_IN  : @t:[12]
// Next_OUT : 

LD R1, b
// Life_IN  : [@c, @d, @i]
// Life_OUT : [@b!!, @c, @d, @i]
// Next_IN  : 
// Next_OUT : @b!!:[15]

LD R0, d
// Life_IN  : [@b!!, @c, @d, @i]
// Life_OUT : [@b!!, @c, @d, @d!, @i]
// Next_IN  : @b!!:[15]
// Next_OUT : @b!!:[15], @d!:[15]

MUL R1, R0, R1
// Life_IN  : [@b!!, @c, @d, @d!, @i]
// Life_OUT : [@c, @d, @i, @t4]
// Next_IN  : @b!!:[15], @d!:[15]
// Next_OUT : @t4:[19]

LD R2, a
// Life_IN  : [@c, @d, @i, @t4]
// Life_OUT : [@a!, @c, @d, @i, @t4]
// Next_IN  : @t4:[19]
// Next_OUT : @a!:[18], @t4:[19]

LD R0, c
// Life_IN  : [@a!, @c, @d, @i, @t4]
// Life_OUT : [@a!, @c, @c!, @d, @i, @t4]
// Next_IN  : @a!:[18], @t4:[19]
// Next_OUT : @a!:[18], @c!:[18], @t4:[19]

MUL R0, R0, R2
// Life_IN  : [@a!, @c, @c!, @d, @i, @t4]
// Life_OUT : [@c, @d, @i, @t4, @t5]
// Next_IN  : @a!:[18], @c!:[18], @t4:[19]
// Next_OUT : @t4:[19], @t5:[19]

ADD R0, R1, R0
// Life_IN  : [@c, @d, @i, @t4, @t5]
// Life_OUT : [@c, @d, @i, @t6]
// Next_IN  : @t4:[19], @t5:[19]
// Next_OUT : @t6:[20]

ADD R2, #0, R0
// Life_IN  : [@c, @d, @i, @t6]
// Life_OUT : [@a!, @c, @d, @i]
// Next_IN  : @t6:[20]
// Next_OUT : @a!:[23]

LD R0, t
// Life_IN  : [@a!, @c, @d, @i]
// Life_OUT : [@a!, @c, @d, @i, @t!]
// Next_IN  : @a!:[23]
// Next_OUT : @a!:[23], @t!:[22]

ADD R1, #0, R0
// Life_IN  : [@a!, @c, @d, @i, @t!]
// Life_OUT : [@a!, @b!!, @c, @d, @i]
// Next_IN  : @a!:[23], @t!:[22]
// Next_OUT : @a!:[23], @b!!:[24]

ST a, R2
// Life_IN  : [@a!, @b!!, @c, @d, @i]
// Life_OUT : [@b!!, @c, @d, @i]
// Next_IN  : @a!:[23], @b!!:[24]
// Next_OUT : @b!!:[24]

ST b, R1
// Life_IN  : [@b!!, @c, @d, @i]
// Life_OUT : [@c, @d, @i]
// Next_IN  : @b!!:[24]
// Next_OUT : 

// TODO:: END THE BLOCK 1 HERE ABOVE !

CLEAR

afterIf:
CLEAR

//      t = d*(2*c + d);
//      c = c*c + d*d;
//      d = t;
//      i = i / 2;

// TODO:: PUT THE BLOCK 2 HERE !
LD R0, c
// Life_IN  : [@a, @b]
// Life_OUT : [@a, @b, @c]
// Next_IN  : 
// Next_OUT : @c:[1]

MUL R0, #2, R0
// Life_IN  : [@a, @b, @c]
// Life_OUT : [@a, @b, @t0]
// Next_IN  : @c:[1]
// Next_OUT : @t0:[3]

LD R1, d
// Life_IN  : [@a, @b, @t0]
// Life_OUT : [@a, @b, @d, @t0]
// Next_IN  : @t0:[3]
// Next_OUT : @d:[3], @t0:[3]

ADD R0, R0, R1
// Life_IN  : [@a, @b, @d, @t0]
// Life_OUT : [@a, @b, @t1]
// Next_IN  : @d:[3], @t0:[3]
// Next_OUT : @t1:[5]

LD R1, d
// Life_IN  : [@a, @b, @t1]
// Life_OUT : [@a, @b, @d!, @t1]
// Next_IN  : @t1:[5]
// Next_OUT : @d!:[5], @t1:[5]

MUL R0, R1, R0
// Life_IN  : [@a, @b, @d!, @t1]
// Life_OUT : [@a, @b, @t2]
// Next_IN  : @d!:[5], @t1:[5]
// Next_OUT : @t2:[6]

ADD R0, #0, R0
// Life_IN  : [@a, @b, @t2]
// Life_OUT : [@a, @b, @t]
// Next_IN  : @t2:[6]
// Next_OUT : @t:[7]

ST t, R0
// Life_IN  : [@a, @b, @t]
// Life_OUT : [@a, @b]
// Next_IN  : @t:[7]
// Next_OUT : 

LD R2, c
// Life_IN  : [@a, @b]
// Life_OUT : [@a, @b, @c!]
// Next_IN  : 
// Next_OUT : @c!:[9]

MUL R2, R2, R2
// Life_IN  : [@a, @b, @c!]
// Life_OUT : [@a, @b, @t3]
// Next_IN  : @c!:[9]
// Next_OUT : @t3:[12]

LD R1, d
// Life_IN  : [@a, @b, @t3]
// Life_OUT : [@a, @b, @d!!, @t3]
// Next_IN  : @t3:[12]
// Next_OUT : @d!!:[11], @t3:[12]

MUL R0, R1, R1
// Life_IN  : [@a, @b, @d!!, @t3]
// Life_OUT : [@a, @b, @t3, @t4]
// Next_IN  : @d!!:[11], @t3:[12]
// Next_OUT : @t3:[12], @t4:[12]

ADD R0, R2, R0
// Life_IN  : [@a, @b, @t3, @t4]
// Life_OUT : [@a, @b, @t5]
// Next_IN  : @t3:[12], @t4:[12]
// Next_OUT : @t5:[13]

ADD R2, #0, R0
// Life_IN  : [@a, @b, @t5]
// Life_OUT : [@a, @b, @c!]
// Next_IN  : @t5:[13]
// Next_OUT : @c!:[19]

LD R0, t
// Life_IN  : [@a, @b, @c!]
// Life_OUT : [@a, @b, @c!, @t!]
// Next_IN  : @c!:[19]
// Next_OUT : @c!:[19], @t!:[15]

ADD R1, #0, R0
// Life_IN  : [@a, @b, @c!, @t!]
// Life_OUT : [@a, @b, @c!, @d!!]
// Next_IN  : @c!:[19], @t!:[15]
// Next_OUT : @c!:[19], @d!!:[20]

LD R0, i
// Life_IN  : [@a, @b, @c!, @d!!]
// Life_OUT : [@a, @b, @c!, @d!!, @i]
// Next_IN  : @c!:[19], @d!!:[20]
// Next_OUT : @c!:[19], @d!!:[20], @i:[17]

DIV R0, R0, #2
// Life_IN  : [@a, @b, @c!, @d!!, @i]
// Life_OUT : [@a, @b, @c!, @d!!, @t6]
// Next_IN  : @c!:[19], @d!!:[20], @i:[17]
// Next_OUT : @c!:[19], @d!!:[20], @t6:[18]

ADD R0, #0, R0
// Life_IN  : [@a, @b, @c!, @d!!, @t6]
// Life_OUT : [@a, @b, @c!, @d!!, @i]
// Next_IN  : @c!:[19], @d!!:[20], @t6:[18]
// Next_OUT : @c!:[19], @d!!:[20], @i:[21]

ST c, R2
// Life_IN  : [@a, @b, @c!, @d!!, @i]
// Life_OUT : [@a, @b, @d!!, @i]
// Next_IN  : @c!:[19], @d!!:[20], @i:[21]
// Next_OUT : @d!!:[20], @i:[21]

ST d, R1
// Life_IN  : [@a, @b, @d!!, @i]
// Life_OUT : [@a, @b, @i]
// Next_IN  : @d!!:[20], @i:[21]
// Next_OUT : @i:[21]

ST i, R0
// Life_IN  : [@a, @b, @i]
// Life_OUT : [@a, @b]
// Next_IN  : @i:[21]
// Next_OUT : 

// TODO:: END THE BLOCK 2 HERE ABOVE!




// TODO:: This instruction is just a placeholder to let the code end, remove the code below!
//LD R0, i
//DEC R0
//ST i, R0
// TODO:: Remove the placeholder above of this line!

CLEAR
BR beginWhile

//    return a + b;
printResult:
LD R0, a
LD R1, b
ADD R0, R0, R1
PRINT R0

end:
PRINT "END"