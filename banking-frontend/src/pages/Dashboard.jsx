import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import bankingApi from '../api/api';

const Dashboard = () => {
    const [accountInfo, setAccountInfo] = useState(null);
    const [transfers, setTransfers] = useState([]);
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();

    useEffect(() => {
        const fetchData = async () => {
            try {
                const [accountResponse, transferResponse] = await Promise.all([
                    bankingApi.get('/accounts/me'),
                    bankingApi.get('/transfers/me'),
                ]);

                setAccountInfo(accountResponse.data);
                setTransfers(transferResponse.data?.data || []);

            } catch (error) {
                console.error("Failed to load data:", error);
                if (error.response?.status === 401) {
                    alert("Session has expired. Please log in again.");
                    navigate('/login');
                }
            } finally {
                setLoading(false);
            }
        };

        fetchData();
    }, [navigate]);

    const handleLogout = () => {
        localStorage.removeItem('accessToken');
        navigate('/login');
    };

    const formatDate = (dateStr) => {
        const date = new Date(dateStr);
        return date.toLocaleDateString('ja-JP', {
            year: 'numeric', month: '2-digit', day: '2-digit',
            hour: '2-digit', minute: '2-digit'
        });
    };

    if (loading) return <div style={styles.center}>Loading data...</div>;

    return (
        <div style={styles.container}>
            <header style={styles.header}>
                <h2>My Wallet (SSO Banking)</h2>
                <button onClick={handleLogout} style={styles.logoutBtn}>Logout</button>
            </header>

            {accountInfo ? (
                <div style={styles.card}>
                    <h3>Account Information</h3>
                    <p><strong>Account Number:</strong> {accountInfo.accountNumber}</p>
                    <p><strong>Current Balance:</strong> <span style={styles.balance}>{accountInfo.balance.toLocaleString()} Yen</span></p>
                    <button onClick={() => navigate('/transfer')} style={styles.transferBtn}>Send Money</button>
                </div>
            ) : (
                <p>Account information not found.</p>
            )}

            {/* Transfer History */}
            <div style={styles.historyCard}>
                <h3>Recent Transfers</h3>
                {transfers.length === 0 ? (
                    <p style={styles.emptyMsg}>No transfer history yet.</p>
                ) : (
                    <table style={styles.table}>
                        <thead>
                        <tr>
                            <th style={styles.th}>Date</th>
                            <th style={styles.th}>From</th>
                            <th style={styles.th}>To</th>
                            <th style={styles.th}>Amount</th>
                        </tr>
                        </thead>
                        <tbody>
                        {transfers.slice(0, 10).map((tx) => {
                            const isSender = accountInfo && tx.fromAccountNumber === accountInfo.accountNumber;
                            return (
                                <tr key={tx.transactionId}>
                                    <td style={styles.td}>{formatDate(tx.transferredAt)}</td>
                                    <td style={styles.td}>{tx.fromAccountNumber}</td>
                                    <td style={styles.td}>{tx.toAccountNumber}</td>
                                    <td style={{ ...styles.td, color: isSender ? '#e74c3c' : '#2ecc71', fontWeight: 'bold' }}>
                                        {isSender ? '-' : '+'}{tx.amount.toLocaleString()} Yen
                                    </td>
                                </tr>
                            );
                        })}
                        </tbody>
                    </table>
                )}
            </div>
        </div>
    );
};

const styles = {
    container: { padding: '20px', maxWidth: '600px', margin: '0 auto' },
    header: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '30px' },
    card: { padding: '20px', border: '1px solid #ddd', borderRadius: '12px', backgroundColor: '#fff', boxShadow: '0 2px 8px rgba(0,0,0,0.05)' },
    balance: { fontSize: '24px', color: '#2ecc71', fontWeight: 'bold' },
    logoutBtn: { backgroundColor: '#e74c3c', color: '#fff', border: 'none', padding: '8px 15px', borderRadius: '5px', cursor: 'pointer' },
    transferBtn: { marginTop: '20px', width: '100%', backgroundColor: '#3498db', color: '#fff', border: 'none', padding: '12px', borderRadius: '8px', cursor: 'pointer', fontSize: '16px' },
    center: { display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }
};

export default Dashboard;