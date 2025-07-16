section .bss
print_buffer resb 12
a resd 1
b resd 1
c resd 1
t0 resd 1
section .data
str0 db 'La suma de '
str1 db ' y '
str2 db ' es: '
section .text
global _start
_start:
    mov dword [a], 5
    mov dword [b], 7
    mov eax, [a]
    add eax, [b]
    mov [t0], eax
    mov eax, [t0]
    mov [c], eax
    mov rax, 1
    mov rdi, 1
    mov rsi, str0
    mov rdx, 11
    syscall
    mov eax, [a]
    call _printRAX
    mov rax, 1
    mov rdi, 1
    mov rsi, str1
    mov rdx, 3
    syscall
    mov eax, [b]
    call _printRAX
    mov rax, 1
    mov rdi, 1
    mov rsi, str2
    mov rdx, 5
    syscall
    mov eax, [c]
    call _printRAX
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
