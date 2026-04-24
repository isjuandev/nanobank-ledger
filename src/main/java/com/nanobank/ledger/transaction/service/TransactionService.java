package com.nanobank.ledger.transaction.service;

import java.util.List;

import com.nanobank.ledger.transaction.dto.TransactionFilterDTO;
import com.nanobank.ledger.transaction.dto.TransactionRequestDTO;
import com.nanobank.ledger.transaction.dto.TransactionResponseDTO;
import com.nanobank.ledger.transaction.dto.TransferRequestDTO;

public interface TransactionService {
    TransactionResponseDTO createTransaction(TransactionRequestDTO dto, String userEmail);
    List<TransactionResponseDTO> getTransactions(Long walletId, TransactionFilterDTO filter, String userEmail);
    TransactionResponseDTO transferTransaction(Long transactionId, TransferRequestDTO dto, String userEmail);
    void deleteTransaction(Long id, String userEmail);
}
