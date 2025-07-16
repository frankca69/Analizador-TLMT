section .bss
print_buffer resb 12
a resd 1
b resd 1
c resd 1
t0 resd 1
t1 resd 1
t2 resd 1
section .data
str0 db 'El valor de c es: ', 0xa
str1 db 'c es mayor que 30', 0xa
str2 db 'c no es mayor que 30', 0xa
str3 db 'Esto siempre se ejecuta', 0xa
section .text
global _start
_start:
    mov dword [a], 10
    mov dword [b], 20
    mov eax, [a]
    add eax, [b]
    mov [t0], eax
    mov eax, [t0]
    mov [c], eax
    mov rax, 1
    mov rdi, 1
    mov rsi, str0
    mov rdx, 19
    syscall
    mov eax, [c]
    call _printRAX
    mov eax, [c]
    cmp eax, 30
    jg L157
    mov byte [t1], 0
    jmp L159
L157:
    mov byte [t1], 1
L159:
    mov al, [t1]
    cmp al, 0
    je L121
    mov rax, 1
    mov rdi, 1
    mov rsi, str1
    mov rdx, 19
    syscall
L121:
    mov rax, 1
    mov rdi, 1
    mov rsi, str2
    mov rdx, 22
    syscall
    mov eax, 1
    cmp eax, 1
    je L167
    mov byte [t2], 0
    jmp L169
L167:
    mov byte [t2], 1
L169:
    mov al, [t2]
    cmp al, 0
    je L133
    mov rax, 1
    mov rdi, 1
    mov rsi, str3
    mov rdx, 25
    syscall
L133:
    mov rax, 60
    xor rdi, rdi
    syscall
_printRAX:
    mov rsi, print_buffer + 10
    mov byte [print_buffer + 11], 0xa
    mov r10, 10
_printRAX_loop:
    xor rdx, rdx
    div r10
    add dl, '0'
    mov [rsi], dl
    dec rsi
    test rax, rax
    jnz _printRAX_loop
    inc rsi
    mov rdx, print_buffer + 12
    sub rdx, rsi
    mov rax, 1
    mov rdi, 1
    syscall
    ret
