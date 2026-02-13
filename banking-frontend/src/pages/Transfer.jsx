import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import bankingApi from '../api/api';

const Transfer = () => {
    const [toAccount, setToAccount] = useState('');
    const [amount, setAmount] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const navigate = useNavigate();

    const handleTransfer = async (e) => {
        e.preventDefault();

        if (amount <= 0) return alert("Please enter the correct amount.");

        setIsLoading(true);
        try {
            // Call transfer API (POST /api/v1/banking/transfers)
            await bankingApi.post('/api/v1/banking/transfers', {
                toAccountNumber: toAccount,
                amount: Number(amount),
                transactionId: `TX-${Date.now()}`, // Idempotency Key
                description: "Bank Transfer"
            });

            alert('Transfer completed!');
            navigate('/dashboard');
        } catch (error) {
            console.error("Transfer failed:", error);
            const errorMsg = error.response?.data?.message || 'An error occurred during the transfer.';
            alert(errorMsg);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div style={styles.container}>
            <header style={styles.header}>
                <button onClick={() => navigate('/dashboard')} style={styles.backBtn}>← Go Back</button>
                <h2>Bank Transfer</h2>
            </header>

            <form onSubmit={handleTransfer} style={styles.form}>
                <div style={styles.inputGroup}>
                    <label>Recipient Account Number</label>
                    <input
                        type="text"
                        value={toAccount}
                        onChange={(e) => setToAccount(e.target.value)}
                        placeholder="000-000-000000"
                        style={styles.input}
                        required
                    />
                </div>

                <div style={styles.inputGroup}>
                    <label>Transfer Amount</label>
                    <input
                        type="number"
                        value={amount}
                        onChange={(e) => setAmount(e.target.value)}
                        placeholder="Enter amount to send"
                        style={styles.input}
                        required
                    />
                </div>

                <button type="submit" disabled={isLoading} style={styles.button}>
                    {isLoading ? 'Processing...' : 'Transfer'}
                </button>
            </form>
        </div>
    );
};

const styles = {
    container: { padding: '20px', maxWidth: '500px', margin: '0 auto' },
    header: { display: 'flex', alignItems: 'center', gap: '20px', marginBottom: '30px' },
    backBtn: { background: 'none', border: 'none', cursor: 'pointer', fontSize: '18px' },
    form: { display: 'flex', flexDirection: 'column', gap: '20px' },
    inputGroup: { display: 'flex', flexDirection: 'column', gap: '8px' },
    input: { padding: '12px', borderRadius: '8px', border: '1px solid #ccc', fontSize: '16px' },
    button: { padding: '15px', backgroundColor: '#3498db', color: '#fff', border: 'none', borderRadius: '8px', cursor: 'pointer', fontSize: '18px', fontWeight: 'bold' }
};

export default Transfer;