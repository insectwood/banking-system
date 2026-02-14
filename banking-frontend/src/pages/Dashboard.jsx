import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import bankingApi from '../api/api';

const Dashboard = () => {
    const [accountInfo, setAccountInfo] = useState(null);
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();

    useEffect(() => {
        const fetchAccountData = async () => {
            try {
                // Call the banking server's account information endpoint
                const response = await bankingApi.get('/accounts/me');
                setAccountInfo(response.data);
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

        fetchAccountData();
    }, [navigate]);

    const handleLogout = () => {
        localStorage.removeItem('accessToken');
        navigate('/login');
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
                    <p><strong>Current Balance:</strong> <span style={styles.balance}>{accountInfo.balance.toLocaleString()} 원</span></p>
                    <button onClick={() => navigate('/transfer')} style={styles.transferBtn}>Send Money</button>
                </div>
            ) : (
                <p>Account information not found.</p>
            )}
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