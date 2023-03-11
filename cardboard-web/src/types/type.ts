export type CardId = number

export interface Card {
    name: string
    set: string
    numberInSet: number
}

export type Library = [[CardId, Card]?]