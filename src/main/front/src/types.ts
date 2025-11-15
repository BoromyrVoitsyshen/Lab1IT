// Цей тип описує структуру даних файлу, яку ми отримуємо з сервера
export type Meta = {
    id: string;
    name: string;
    createdAt: string; // Дати надходять як рядки (string)
    modifiedAt: string;
    uploadedBy: string;
    editedBy: string;
    size: number;
};

// Наші нові типи фільтрів для варіанту 7
export type Filter = 'ALL' | 'C' | 'JPG';