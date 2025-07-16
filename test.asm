section .bss
print_buffer resb 12
section .data
    a dd 0
    b dd 0
    c dd 0
    msg0 db "La suma de ", 0
    msg1 db " y ", 0
    msg2 db " es: ", 0
    newline db 10, 0
    buffer db 12 dup(0)

section .text
    global _start

_start:
    mov dword [a], 5
    mov dword [b], 7
    mov eax, [a]
    add eax, [b]
    mov [c], eax
    mov rax, 1
    mov rdi, 1
    mov rsi, msg0
    mov rdx, 11
    syscall
    mov eax, [a]
    call print_number
    mov rax, 1
    mov rdi, 1
    mov rsi, msg1
    mov rdx, 3
    syscall
    mov eax, [b]
    call print_number
    mov rax, 1
    mov rdi, 1
    mov rsi, msg2
    mov rdx, 5
    syscall
    mov eax, [c]
    call print_number
    mov rax, 1
    mov rdi, 1
    mov rsi, newline
    mov rdx, 1
    syscall
    mov rax, 60
    mov rdi, 0
    syscall

print_number:
    push rbx
    push rcx
    push rdx
    push rsi

    mov ebx, 10
    mov ecx, 0
    mov rsi, buffer + 11
    mov byte [rsi], 0

    cmp eax, 0
    jne convert_loop
    dec rsi
    mov byte [rsi], '0'
    inc ecx
    jmp print_it

convert_loop:
    cmp eax, 0
    je print_it

    xor edx, edx
    div ebx
    add dl, '0'
    dec rsi
    mov [rsi], dl
    inc ecx
    jmp convert_loop

print_it:
    mov rax, 1
    mov rdi, 1
    mov rdx, rcx
    syscall

    pop rsi
    pop rdx
    pop rcx
    pop rbx
    ret
